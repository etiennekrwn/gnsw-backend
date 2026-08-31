package com.gnsw.gnsw_backend.util;

/**
 * Centralized membership pricing.
 *
 * Amounts are in kobo (Paystack's smallest NGN unit): ₦21,000 = 2,100,000 kobo.
 * These are the ANNUAL membership dues. Under the free-application model the
 * first year is collected only after the applicant is accepted (persistent
 * pay-wall on the members portal), then auto-renews annually via a Paystack
 * subscription created from the authorization captured on that first payment.
 */
public final class MembershipFees {

    private MembershipFees() {
    }

    /** Annual fee in kobo for a given tier. Throws for tiers without a purchasable plan. */
    public static int annualFeeKobo(String tierName) {
        return switch (tierName) {
            case "AFFILIATE" -> 2100000;
            case "ASSOCIATE" -> 3000000;
            case "MEMBER" -> 5000000;
            default -> throw new IllegalArgumentException("Tier has no annual membership fee: " + tierName);
        };
    }

    /** Human-readable NGN string for emails/display, e.g. "₦30,000" for ASSOCIATE. */
    public static String annualFeeLabel(String tierName) {
        int kobo = annualFeeKobo(tierName);
        return "₦" + (kobo / 100);
    }
}