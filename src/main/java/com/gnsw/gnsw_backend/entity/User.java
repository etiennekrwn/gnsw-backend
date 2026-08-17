package com.gnsw.gnsw_backend.entity;

import com.gnsw.gnsw_backend.enums.MembershipTier;
import com.gnsw.gnsw_backend.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "address_line1", nullable = false)
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(nullable = false)
    private String city;

    @Column(name = "state_province", nullable = false)
    private String stateProvince;

    @Column(name = "zip_postal_code", nullable = false)
    private String zipPostalCode;

    @Column(nullable = false)
    private String country;

    @Column(unique = true)
    private String username;

    @Column(name = "password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MembershipTier tier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Column(nullable = false)
    private String role;

    @Column(name = "professional_id", unique = true)
    private String professionalId;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Column(name = "rejected_by")
    private UUID rejectedBy;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "password_set_token")
    private String passwordSetToken;

    @Column(name = "password_set_token_expires_at")
    private LocalDateTime passwordSetTokenExpiresAt;

    @Column(name = "password_set_at")
    private LocalDateTime passwordSetAt;

    @Column(name = "password_reset_token")
    private String passwordResetToken;

    @Column(name = "password_reset_token_expires_at")
    private LocalDateTime passwordResetTokenExpiresAt;

    @Column(name = "activation_reminder_count")
    private Integer activationReminderCount;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = UserStatus.PENDING;
        if (role == null) role = "ROLE_MEMBER";
        // The column is NOT NULL (migration V7); Hibernate includes it in the
        // INSERT, so we must default it or the persist fails with a 23502.
        if (activationReminderCount == null) activationReminderCount = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}