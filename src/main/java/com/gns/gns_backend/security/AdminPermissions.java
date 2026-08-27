package com.gns.gns_backend.security;

import com.gns.gns_backend.entity.AdminUser;
import com.gns.gns_backend.enums.AdminRole;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Module-level permission model for the admin console.
 *
 * A SUPER_ADMIN or ADMIN implicitly holds every module permission. A MANAGER
 * holds only the modules that were ticked when they were created. The JWT
 * filter converts these into Spring authorities on every request, so changes
 * take effect immediately and server-side (UI hiding is never the boundary).
 */
public final class AdminPermissions {

    /** Marker granted only to admin-console identities (never members). */
    public static final String ADMIN_IDENTITY = "ADMIN_IDENTITY";

    public static final String MEMBERS = "MODULE_MEMBERS";
    public static final String CONTENT = "MODULE_CONTENT";
    public static final String EVENTS = "MODULE_EVENTS";
    public static final String COURSES = "MODULE_COURSES";
    public static final String PORTAL = "MODULE_PORTAL";
    public static final String COMMUNICATIONS = "MODULE_COMMUNICATIONS";
    public static final String MEDIA = "MODULE_MEDIA";
    public static final String REPORTS = "MODULE_REPORTS";
    public static final String SETTINGS = "MODULE_SETTINGS";
    public static final String ADMIN_USERS = "MODULE_ADMIN_USERS";

    public static final Set<String> ALL_MODULES = Set.of(
            MEMBERS, CONTENT, EVENTS, COURSES, PORTAL,
            COMMUNICATIONS, MEDIA, REPORTS, SETTINGS, ADMIN_USERS);

    /** Modules a MANAGER is allowed to be granted (ADMIN_USERS is admin-only). */
    public static final Set<String> MANAGER_ASSIGNABLE = Set.of(
            MEMBERS, CONTENT, EVENTS, COURSES, PORTAL,
            COMMUNICATIONS, MEDIA, REPORTS, SETTINGS);

    private AdminPermissions() {
    }

    /** The module keys an admin may access (all for SUPER_ADMIN/ADMIN). */
    public static Set<String> allowedModules(AdminUser user) {
        if (user.getRole() == AdminRole.SUPER_ADMIN || user.getRole() == AdminRole.ADMIN) {
            return ALL_MODULES;
        }
        if (user.getAllowedModules() == null || user.getAllowedModules().isBlank()) {
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(Arrays.asList(user.getAllowedModules().split(",")));
    }

    /** Full Spring authority set for an admin, resolved fresh each request. */
    public static Set<String> authoritiesFor(AdminUser user) {
        Set<String> authorities = new LinkedHashSet<>();
        authorities.add(ADMIN_IDENTITY);
        switch (user.getRole()) {
            case SUPER_ADMIN -> authorities.add("ROLE_SUPER_ADMIN");
            case ADMIN -> authorities.add("ROLE_ADMIN");
            default -> authorities.add("ROLE_MANAGER");
        }
        allowedModules(user).forEach(authorities::add);
        return authorities;
    }
}