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

    @Column(columnDefinition = "TEXT")
    private String sectors;

    @Column(name = "speech_types", columnDefinition = "TEXT")
    private String speechTypes;

    @Column(columnDefinition = "TEXT")
    private String languages;

    private String zone;

    @Column(name = "linkedin_profile", length = 500)
    private String linkedInProfile;

    @Column(name = "socials", columnDefinition = "TEXT")
    private String socials;

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