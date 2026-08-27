package com.gnsw.gnsw_backend.controller;

import com.gnsw.gnsw_backend.entity.EmailOtp;
import com.gnsw.gnsw_backend.repository.EmailOtpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * DEVELOPMENT-ONLY controller.
 * Provides a way to retrieve OTP codes for testing when email is not configured.
 * Remove this file before production deployment.
 */
@RestController
@RequestMapping("/api/v1/dev")
@Profile("dev")
@RequiredArgsConstructor
public class DevController {

    private final EmailOtpRepository emailOtpRepository;

    @GetMapping("/otp/{email}")
    public ResponseEntity<Map<String, Object>> getLatestOtp(@PathVariable String email) {
        var otpOpt = emailOtpRepository
                .findTopByEmailAndVerifiedAtIsNullOrderByCreatedAtDesc(email);

        if (otpOpt.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "found", false,
                    "message", "No pending OTP found for " + email
            ));
        }

        EmailOtp otp = otpOpt.get();
        return ResponseEntity.ok(Map.of(
                "found", true,
                "email", otp.getEmail(),
                "otpCode", otp.getOtpCode(),
                "expiresAt", otp.getExpiresAt().toString(),
                "attempts", otp.getAttempts()
        ));
    }
}