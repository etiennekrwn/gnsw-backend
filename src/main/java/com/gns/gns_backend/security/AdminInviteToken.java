package com.gns.gns_backend.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Capability-token helpers for admin invitations. The raw token travels only
 * in the emailed link; only its SHA-256 hash is persisted, so a DB leak does
 * not reveal usable tokens.
 */
public final class AdminInviteToken {

    private static final SecureRandom RANDOM = new SecureRandom();

    private AdminInviteToken() {
    }

    /** 32 random bytes, base64url (unguessable, single-use capability token). */
    public static String generateRaw() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 hex digest (store this, never the raw token). */
    public static String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}