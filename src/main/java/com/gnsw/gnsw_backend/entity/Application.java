package com.gnsw.gnsw_backend.entity;

import com.gnsw.gnsw_backend.enums.ApplicationStatus;
import com.gnsw.gnsw_backend.enums.MembershipTier;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "applications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "membership_tier", nullable = false)
    private MembershipTier membershipTier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Column(name = "phone")
    private String phone;

    @Column(name = "social_media_platform", columnDefinition = "TEXT")
    private String socialMediaPlatform;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(name = "reason_for_joining", columnDefinition = "TEXT")
    private String reasonForJoining;

    @Column(name = "current_professional_role", length = 200)
    private String currentProfessionalRole;

    @Column(name = "favourite_orator", length = 300)
    private String favouriteOrator;

    @Column(name = "speechwriting_training", length = 10)
    private String speechwritingTraining;

    @Column(name = "training_details", columnDefinition = "TEXT")
    private String trainingDetails;

    @Column(name = "highest_qualification", length = 100)
    private String highestQualification;

    @Column(name = "current_job_title", length = 200)
    private String currentJobTitle;

    @Column(name = "current_organization", length = 200)
    private String currentOrganization;

    @Column(name = "payment_reference", unique = true)
    private String paymentReference;

    @Column(name = "payment_amount")
    private Integer paymentAmount;

    @Column(name = "payment_status")
    private String paymentStatus;

    @Column(name = "subscription_code")
    private String subscriptionCode;

    @Column(name = "plan_code")
    private String planCode;

    @Column(name = "email_token")
    private String emailToken;

    @Column(name = "authorization_code")
    private String authorizationCode;

    @Column(name = "email_verified")
    private boolean emailVerified;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "rejection_reason")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = ApplicationStatus.PENDING;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}