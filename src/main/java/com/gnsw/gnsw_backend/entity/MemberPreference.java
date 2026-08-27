package com.gnsw.gnsw_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A member's own portal preferences. One row per member (keyed by user_id).
 * This is distinct from Guild-wide settings ({@link GuildSetting}) and is
 * edited only by the member themselves.
 */
@Entity
@Table(name = "member_preferences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MemberPreference {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "email_notifications")
    private Boolean emailNotifications;

    @Column(name = "weekly_digest")
    private Boolean weeklyDigest;

    @Column(name = "new_article_alerts")
    private Boolean newArticleAlerts;

    @Column(name = "comment_alerts")
    private Boolean commentAlerts;

    @Column(name = "member_announcements")
    private Boolean memberAnnouncements;

    @Column(name = "dark_mode")
    private Boolean darkMode;

    @Column(name = "font_size")
    private String fontSize;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedAt = LocalDateTime.now();
        if (emailNotifications == null) emailNotifications = true;
        if (weeklyDigest == null) weeklyDigest = false;
        if (newArticleAlerts == null) newArticleAlerts = true;
        if (commentAlerts == null) commentAlerts = true;
        if (memberAnnouncements == null) memberAnnouncements = false;
        if (darkMode == null) darkMode = false;
        if (fontSize == null || fontSize.isBlank()) fontSize = "medium";
    }
}