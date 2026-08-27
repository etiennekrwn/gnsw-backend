package com.gnsw.gnsw_backend.util;

import java.security.SecureRandom;

public class PaymentReferenceGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateReference() {
        int randomPart = 100000 + RANDOM.nextInt(900000);
        String timePart = String.valueOf(System.currentTimeMillis());
        String last4 = timePart.substring(timePart.length() - 4);
        return "GNS-" + randomPart + "-" + last4;
    }
}