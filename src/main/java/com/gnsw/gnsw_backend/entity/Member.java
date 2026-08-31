package com.gnsw.gnsw_backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "members")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    private String organisation;

    @Column(columnDefinition = "TEXT")
    private String bio;

    private String phone;

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

    private String zone;

    @Column(name = "social_media_platform", columnDefinition = "TEXT")
    private String socialMediaPlatform;

    @Column(name = "reason_for_joining", columnDefinition = "TEXT")
    private String reasonForJoining;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}