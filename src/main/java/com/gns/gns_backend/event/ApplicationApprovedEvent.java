package com.gns.gns_backend.event;

/**
 * Published when an application is approved.
 * Consumed AFTER_COMMIT so the welcome email is only sent once the
 * approval transaction has successfully committed.
 */
public record ApplicationApprovedEvent(
        String email,
        String firstName,
        String tier,
        String professionalId,
        String token,
        String customMessage) {
}
