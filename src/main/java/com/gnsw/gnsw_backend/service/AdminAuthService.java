package com.gnsw.gnsw_backend.service;

import com.gnsw.gnsw_backend.entity.AdminUser;
import com.gnsw.gnsw_backend.enums.AdminStatus;
import com.gnsw.gnsw_backend.repository.AdminUserRepository;
import com.gnsw.gnsw_backend.security.AdminInviteToken;
import com.gnsw.gnsw_backend.security.AdminPermissions;
import com.gnsw.gnsw_backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /** Logs an admin into the console using their separate admin_users identity. */
    public Map<String, Object> login(String email, String password) {
        AdminUser user = adminUserRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password."));

        if (user.getPasswordHash() == null || user.getStatus() == AdminStatus.INVITED) {
            throw new IllegalArgumentException("Account not activated. Use the invite link sent to your email.");
        }
        if (user.getStatus() == AdminStatus.DISABLED) {
            throw new IllegalArgumentException("This account has been disabled. Contact the Super Admin.");
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password.");
        }

        user.setLastLoginAt(LocalDateTime.now());
        adminUserRepository.save(user);

        String token = jwtTokenProvider.generateToken(user.getEmail(), user.getRole().name(), "ADMIN");

        Map<String, Object> userMap = new LinkedHashMap<>();
        userMap.put("id", user.getId());
        userMap.put("email", user.getEmail());
        userMap.put("displayName", user.getDisplayName());
        userMap.put("role", user.getRole().name());
        userMap.put("status", user.getStatus().name());
        userMap.put("theme", user.getTheme() != null ? user.getTheme() : "LIGHT");
        userMap.put("allowedModules", new ArrayList<>(AdminPermissions.allowedModules(user)));

        Map<String, Object> result = new HashMap<>();
        result.put("accessToken", token);
        result.put("tokenType", "Bearer");
        result.put("expiresIn", 86400);
        result.put("user", userMap);
        return result;
    }

    /** Validates an invite token without consuming it. */
    public Map<String, Object> validateInvite(String token) {
        if (token == null || token.isBlank()) {
            return Map.of("valid", false, "message", "Missing invite token.");
        }
        AdminUser admin = adminUserRepository.findByInviteTokenHash(AdminInviteToken.sha256(token))
                .orElse(null);
        if (admin == null) {
            return Map.of("valid", false, "message", "Invite link is invalid. Please contact the Super Admin.");
        }
        if (admin.getStatus() != AdminStatus.INVITED) {
            return Map.of("valid", false, "message", "This invite has already been used.");
        }
        if (admin.getInviteTokenExpiresAt() == null
                || admin.getInviteTokenExpiresAt().isBefore(LocalDateTime.now())) {
            return Map.of("valid", false, "message", "This invite link has expired. Ask the Super Admin to resend.");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("valid", true);
        data.put("email", admin.getEmail());
        data.put("displayName", admin.getDisplayName());
        data.put("role", admin.getRole().name());
        data.put("roleLabel", AdminLabels.roleLabel(admin.getRole()));
        data.put("allowedModules", new ArrayList<>(AdminPermissions.allowedModules(admin)));
        return data;
    }

    public void acceptInvite(String token, String email, String password, String confirmation) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
        if (!password.equals(confirmation)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }
        AdminUser admin = adminUserRepository.findByInviteTokenHash(AdminInviteToken.sha256(token))
                .orElseThrow(() -> new IllegalArgumentException("Invite link is invalid. Please contact the Super Admin."));
        if (admin.getStatus() != AdminStatus.INVITED) {
            throw new IllegalArgumentException("This invite has already been used.");
        }
        if (admin.getInviteTokenExpiresAt() == null
                || admin.getInviteTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("This invite link has expired. Please ask the Super Admin to resend.");
        }
        // Email-match defense: the token is bound to the invited inbox. Reject a
        // mismatch so a mistyped/leaked invite can never be accepted elsewhere.
        if (!admin.getEmail().equalsIgnoreCase(email.trim())) {
            throw new IllegalArgumentException("This invite belongs to a different email address.");
        }

        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setStatus(AdminStatus.ACTIVE);
        admin.setActivatedAt(LocalDateTime.now());
        admin.setInviteTokenHash(null);
        admin.setInviteTokenExpiresAt(null);
        adminUserRepository.save(admin);
    }
}