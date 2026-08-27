package com.gns.gns_backend.controller;

import com.gns.gns_backend.dto.response.ApiResponse;
import com.gns.gns_backend.entity.GuildSetting;
import com.gns.gns_backend.repository.GuildSettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Guild-wide global settings (edited by admins). A single singleton row.
 * Distinct from per-member preferences.
 */
@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminSettingsController {

    private final GuildSettingRepository guildSettingRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSettings() {
        GuildSetting g = ensureSingleton();
        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Settings retrieved.")
                        .data(toMap(g))
                        .build());
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateSettings(
            @RequestBody Map<String, Object> body) {
        GuildSetting g = ensureSingleton();
        applyString(body, "portalName", g::setPortalName);
        applyString(body, "accentColor", g::setAccentColor);
        applyString(body, "footerText", g::setFooterText);
        applyString(body, "guildName", g::setGuildName);
        applyString(body, "contactEmail", g::setContactEmail);
        applyString(body, "contactPhone", g::setContactPhone);
        applyString(body, "address", g::setAddress);
        applyString(body, "idPrefix", g::setIdPrefix);
        applyBoolean(body, "registrationOpen", g::setRegistrationOpen);
        guildSettingRepository.save(g);

        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Settings updated.")
                        .data(toMap(g))
                        .build());
    }

    private GuildSetting ensureSingleton() {
        return guildSettingRepository.findById(GuildSetting.SINGLETON_ID)
                .orElseGet(() -> guildSettingRepository.save(GuildSetting.builder()
                        .id(GuildSetting.SINGLETON_ID)
                        .registrationOpen(true)
                        .portalName("GNS Members Portal")
                        .accentColor("#111418")
                        .footerText("© 2026 Guild of Nigerian Speechwriters. All rights reserved.")
                        .guildName("Guild of Nigerian Speechwriters")
                        .idPrefix("GNS")
                        .build()));
    }

    private Map<String, Object> toMap(GuildSetting g) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", g.getId());
        m.put("registrationOpen", g.getRegistrationOpen());
        m.put("portalName", g.getPortalName());
        m.put("accentColor", g.getAccentColor());
        m.put("footerText", g.getFooterText());
        m.put("guildName", g.getGuildName());
        m.put("contactEmail", g.getContactEmail());
        m.put("contactPhone", g.getContactPhone());
        m.put("address", g.getAddress());
        m.put("idPrefix", g.getIdPrefix());
        m.put("updatedAt", g.getUpdatedAt());
        return m;
    }

    private void applyString(Map<String, Object> body, String key, java.util.function.Consumer<String> setter) {
        Object v = body.get(key);
        if (v instanceof String s) setter.accept(s);
    }

    private void applyBoolean(Map<String, Object> body, String key, java.util.function.Consumer<Boolean> setter) {
        Object v = body.get(key);
        if (v instanceof Boolean b) setter.accept(b);
    }
}