package com.gnsw.gnsw_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Guild-wide global settings edited by admins. A singleton row (id = 1).
 * This is distinct from per-member preferences ({@code MemberPreference}).
 */
@Entity
@Table(name = "guild_settings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GuildSetting {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(name = "registration_enabled")
    private Boolean registrationOpen;

    @Column(name = "portal_name")
    private String portalName;

    @Column(name = "accent_color")
    private String accentColor;

    @Column(name = "footer_text")
    private String footerText;

    @Column(name = "guild_name")
    private String guildName;

    @Column(name = "contact_email")
    private String contactEmail;

    @Column(name = "contact_phone")
    private String contactPhone;

    @Column(name = "address")
    private String address;

    @Column(name = "id_prefix")
    private String idPrefix;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedAt = LocalDateTime.now();
        if (id == null) id = SINGLETON_ID;
        if (registrationOpen == null) registrationOpen = true;
        if (portalName == null || portalName.isBlank()) portalName = "GNS Members Portal";
        if (accentColor == null || accentColor.isBlank()) accentColor = "#111418";
        if (guildName == null || guildName.isBlank()) guildName = "Guild of Nigerian Speechwriters";
        if (idPrefix == null || idPrefix.isBlank()) idPrefix = "GNS";
    }
}