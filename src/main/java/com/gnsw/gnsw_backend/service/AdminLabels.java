package com.gnsw.gnsw_backend.service;

import com.gnsw.gnsw_backend.enums.AdminRole;
import com.gnsw.gnsw_backend.security.AdminPermissions;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Human-readable labels for admin roles and module permissions. Used in
 * onboarding emails and, with the matching frontend map, to render chips.
 */
public final class AdminLabels {

    public static final Map<String, String> MODULE_LABELS = Map.of(
            AdminPermissions.MEMBERS, "Members",
            AdminPermissions.CONTENT, "Blog & Events",
            AdminPermissions.EVENTS, "Events",
            AdminPermissions.COURSES, "Courses",
            AdminPermissions.PORTAL, "Portal (Articles & Announcements)",
            AdminPermissions.COMMUNICATIONS, "Communications",
            AdminPermissions.MEDIA, "Media Library",
            AdminPermissions.REPORTS, "Reports",
            AdminPermissions.SETTINGS, "Settings",
            AdminPermissions.ADMIN_USERS, "Admin Users"
    );

    private AdminLabels() {
    }

    public static String roleLabel(AdminRole role) {
        if (role == null) return "Manager";
        return switch (role) {
            case SUPER_ADMIN -> "Super Admin";
            case ADMIN -> "Administrator (full access)";
            case MANAGER -> "Manager";
        };
    }

    public static String modulesLabel(Set<String> modules) {
        if (modules == null || modules.isEmpty()) return "";
        List<String> labels = modules.stream()
                .map(m -> MODULE_LABELS.getOrDefault(m, m))
                .sorted()
                .toList();
        return String.join(", ", labels);
    }
}