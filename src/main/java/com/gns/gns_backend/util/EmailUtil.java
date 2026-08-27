package com.gns.gns_backend.util;

public final class EmailUtil {

    private EmailUtil() {
    }

    /**
     * Normalizes an email for storage and lookup (trim + lowercase).
     * Prevents case-sensitive duplicates (Jane@X.com vs jane@x.com).
     */
    public static String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
