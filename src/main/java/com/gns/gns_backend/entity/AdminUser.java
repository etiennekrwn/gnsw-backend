package com.gns.gns_backend.entity;

import com.gns.gns_backend.enums.AdminRole;
import com.gns.gns_backend.enums.AdminStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A GNS admin-console account. Fully decoupled from the member {@link User}
 * table: admins log in through a separate portal with their own identity.
 *
 * Role model:
 *  - SUPER_ADMIN: exactly one, immutable, full access, cannot be deleted.
 *  - ADMIN:       full access (same surface as SUPER_ADMIN) but deletable and
 *                 able to invite/adjust managers.
 *  - MANAGER:     restricted to the modules listed in {@link #allowedModules}.
 *
 * Invite lifecycle: an account is created as INVITED with a one-time token that
 * is emailed. The recipient sets their own password to become ACTIVE.
 */
@Entity
@Table(name = "admin_users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminRole role;

    /** Comma-separated module keys for MANAGER accounts; null = full access. */
    @Column(name = "allowed_modules")
    private String allowedModules;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AdminStatus status;

    @Column(name = "invite_token_hash", length = 64)
    private String inviteTokenHash;

    @Column(name = "invite_token_expires_at")
    private LocalDateTime inviteTokenExpiresAt;

    @Column(name = "invited_by")
    private UUID invitedBy;

    @Column(name = "invited_at")
    private LocalDateTime invitedAt;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(nullable = false, length = 10)
    private String theme = "LIGHT";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (role == null) role = AdminRole.MANAGER;
        if (status == null) status = AdminStatus.INVITED;
        if (theme == null || theme.isBlank()) theme = "LIGHT";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}