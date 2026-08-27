package com.gns.gns_backend.service;

import com.gns.gns_backend.entity.AdminUser;
import com.gns.gns_backend.enums.AdminRole;
import com.gns.gns_backend.enums.AdminStatus;
import com.gns.gns_backend.repository.AdminUserRepository;
import com.gns.gns_backend.security.AdminInviteToken;
import com.gns.gns_backend.security.AdminPermissions;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserManagementService {

    private static final int INVITE_HOURS = 72;

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /** Guard: only SUPER_ADMIN / ADMIN may manage admin accounts. */
    private void requireFullAdmin(AdminUser actor) {
        if (actor.getRole() == AdminRole.MANAGER) {
            throw new IllegalArgumentException("Managers cannot manage admin accounts.");
        }
    }

    public List<Map<String, Object>> list() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AdminUser a : adminUserRepository.findAllByOrderByCreatedAtDesc()) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("email", a.getEmail());
            m.put("displayName", a.getDisplayName());
            m.put("role", a.getRole().name());
            m.put("roleLabel", AdminLabels.roleLabel(a.getRole()));
            m.put("status", a.getStatus().name());
            m.put("allowedModules", new ArrayList<>(AdminPermissions.allowedModules(a)));
            m.put("createdAt", a.getCreatedAt());
            m.put("activatedAt", a.getActivatedAt());
            m.put("lastLoginAt", a.getLastLoginAt());
            m.put("invitedBy", a.getInvitedBy());
            result.add(m);
        }
        return result;
    }

    /** Creates an INVITED admin and emails their one-time onboarding link. */
    public Map<String, Object> invite(AdminUser actor, String email, String displayName,
                            String roleName, Set<String> modules) {
        requireFullAdmin(actor);
        AdminRole role;
        try {
            role = AdminRole.valueOf(roleName.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Role must be ADMIN or MANAGER.");
        }
        if (role == AdminRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("You cannot create another Super Admin.");
        }

        String normalizedEmail = email.trim().toLowerCase();
        if (adminUserRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("An admin account already exists for that email.");
        }

        String modulesCsv = null;
        if (role == AdminRole.MANAGER) {
            if (modules == null || modules.isEmpty()) {
                throw new IllegalArgumentException("Select at least one module for a manager.");
            }
            modulesCsv = joinValidModules(modules);
        }

        LocalDateTime now = LocalDateTime.now();
        String raw = AdminInviteToken.generateRaw();
        AdminUser created = AdminUser.builder()
                .email(normalizedEmail)
                .displayName(displayName.trim())
                .role(role)
                .status(AdminStatus.INVITED)
                .allowedModules(modulesCsv)
                .invitedBy(actor.getId())
                .invitedAt(now)
                .inviteTokenHash(AdminInviteToken.sha256(raw))
                .inviteTokenExpiresAt(now.plusHours(INVITE_HOURS))
                .build();
        adminUserRepository.save(created);

        String label = role == AdminRole.ADMIN
                ? "Full access (all modules)"
                : AdminLabels.modulesLabel(AdminPermissions.allowedModules(created));
        emailService.sendAdminInvite(created.getEmail(), created.getDisplayName(),
                AdminLabels.roleLabel(role), label, raw);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", created.getId());
        result.put("email", created.getEmail());
        return result;
    }

    public void resendInvite(AdminUser actor, UUID id) {
        requireFullAdmin(actor);
        AdminUser target = get(id);
        if (target.getStatus() != AdminStatus.INVITED) {
            throw new IllegalArgumentException("Only pending invites can be resent.");
        }
        LocalDateTime now = LocalDateTime.now();
        String raw = AdminInviteToken.generateRaw();
        target.setInviteTokenHash(AdminInviteToken.sha256(raw));
        target.setInviteTokenExpiresAt(now.plusHours(INVITE_HOURS));
        adminUserRepository.save(target);
        String label = target.getRole() == AdminRole.ADMIN
                ? "Full access (all modules)"
                : AdminLabels.modulesLabel(AdminPermissions.allowedModules(target));
        emailService.sendAdminInvite(target.getEmail(), target.getDisplayName(),
                AdminLabels.roleLabel(target.getRole()), label, raw);
    }

    public void revokeInvite(AdminUser actor, UUID id) {
        requireFullAdmin(actor);
        AdminUser target = get(id);
        if (target.getStatus() != AdminStatus.INVITED) {
            throw new IllegalArgumentException("Only pending invites can be revoked.");
        }
        target.setInviteTokenHash(null);
        target.setInviteTokenExpiresAt(null);
        adminUserRepository.save(target);
    }

    public void disable(AdminUser actor, UUID id) {
        requireFullAdmin(actor);
        AdminUser target = get(id);
        if (target.getRole() == AdminRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("The Super Admin cannot be disabled.");
        }
        if (target.getStatus() == AdminStatus.ACTIVE) {
            target.setStatus(AdminStatus.DISABLED);
            adminUserRepository.save(target);
        }
    }

    public void enable(AdminUser actor, UUID id) {
        requireFullAdmin(actor);
        AdminUser target = get(id);
        if (target.getStatus() == AdminStatus.DISABLED) {
            target.setStatus(AdminStatus.ACTIVE);
            adminUserRepository.save(target);
        }
    }

    public void setRole(AdminUser actor, UUID id, String roleName, Set<String> modules) {
        requireFullAdmin(actor);
        AdminUser target = get(id);
        if (target.getRole() == AdminRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("You cannot change the Super Admin's role.");
        }
        if (target.getId().equals(actor.getId())) {
            throw new IllegalArgumentException("You cannot change your own role.");
        }
        AdminRole role;
        try {
            role = AdminRole.valueOf(roleName.toUpperCase().trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Role must be ADMIN or MANAGER.");
        }
        if (role == AdminRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("You cannot promote to Super Admin.");
        }
        target.setRole(role);
        target.setAllowedModules(role == AdminRole.MANAGER ? joinValidModules(modules) : null);
        adminUserRepository.save(target);
    }

    public void delete(AdminUser actor, UUID id) {
        requireFullAdmin(actor);
        AdminUser target = get(id);
        if (target.getRole() == AdminRole.SUPER_ADMIN) {
            throw new IllegalArgumentException("The Super Admin can never be deleted.");
        }
        boolean isSelf = target.getId().equals(actor.getId());
        if (actor.getRole() == AdminRole.ADMIN && !isSelf && target.getRole() != AdminRole.MANAGER) {
            throw new IllegalArgumentException("Admins can delete themselves or manager accounts.");
        }
        adminUserRepository.delete(target);
    }

    public void changeOwnPassword(AdminUser actor, String current, String newPassword, String confirmation) {
        if (actor.getPasswordHash() == null || !passwordEncoder.matches(current, actor.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters.");
        }
        if (!newPassword.equals(confirmation)) {
            throw new IllegalArgumentException("New passwords do not match.");
        }
        actor.setPasswordHash(passwordEncoder.encode(newPassword));
        adminUserRepository.save(actor);
    }

    private AdminUser get(UUID id) {
        return adminUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Admin account not found."));
    }

    private String joinValidModules(Set<String> modules) {
        if (modules == null || modules.isEmpty()) {
            throw new IllegalArgumentException("Select at least one module.");
        }
        List<String> valid = modules.stream()
                .filter(AdminPermissions.MANAGER_ASSIGNABLE::contains)
                .distinct()
                .sorted()
                .toList();
        if (valid.size() != modules.size()) {
            throw new IllegalArgumentException("One or more selected modules are not recognised.");
        }
        return String.join(",", valid);
    }
}