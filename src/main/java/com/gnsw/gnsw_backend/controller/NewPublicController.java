package com.gnsw.gnsw_backend.controller;

import com.gnsw.gnsw_backend.dto.response.ApiResponse;
import com.gnsw.gnsw_backend.service.NewApplicationService;
import com.gnsw.gnsw_backend.service.OtpService;
import com.gnsw.gnsw_backend.util.EmailUtil;
import com.gnsw.gnsw_backend.util.PaymentReferenceGenerator;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * NEW flow controller.
 * Step 1: Form data held in frontend (no backend call)
 * Step 2: Send OTP → Verify OTP (by email)
 * Step 3: Payment → On success, create Application record
 */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class NewPublicController {

    private final OtpService otpService;
    private final NewApplicationService newApplicationService;

    /**
     * Send OTP to email (no user creation needed).
     * Checks for duplicate email before sending OTP.
     */
    @PostMapping("/send-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendOtp(@Valid @RequestBody SendOtpRequest request) {
        String email = EmailUtil.normalize(request.getEmail());
        // Check if email already has an application or user account
        newApplicationService.checkEmailAvailable(email);
        long resendAvailableAt = otpService.generateAndSendOtp(email);
        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("OTP sent to your email.")
                        .data(Map.of("email", email, "resendAvailableAt", resendAvailableAt))
                        .build());
    }

    /**
     * Verify OTP by email.
     */
    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        String email = EmailUtil.normalize(request.getEmail());
        // Re-check availability: the email may have become unavailable (an
        // application or account may have been created) on the OTP step.
        newApplicationService.checkEmailAvailable(email);
        otpService.verifyOtp(email, request.getOtpCode());

        String paymentRef = PaymentReferenceGenerator.generateReference();

        // Calculate amount based on tier
        int amount = switch (request.getMembershipTier()) {
            case "AFFILIATE" -> 2100000;
            case "ASSOCIATE" -> 3000000;
            case "MEMBER" -> 5000000;
            default -> throw new IllegalArgumentException("Invalid tier.");
        };

        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Email verified successfully. Proceed to payment.")
                        .data(Map.of(
                                "email", email,
                                "tier", request.getMembershipTier(),
                                "amount", amount,
                                "paymentReference", paymentRef
                        ))
                        .build());
    }

    /**
     * Create application AFTER successful payment.
     */
    @PostMapping("/applications")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createApplication(
            @Valid @RequestBody CreateApplicationRequest request) {
        var application = newApplicationService.createApplication(
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                request.getAddressLine1(),
                request.getAddressLine2(),
                request.getCity(),
                request.getStateProvince(),
                request.getZipPostalCode(),
                request.getCountry(),
                request.getPhone(),
                request.getLinkedInProfile(),
                request.getSocials(),
                request.getBio(),
                request.getReasonForJoining(),
                request.getSectors(),
                request.getSpeechTypes(),
                request.getLanguages(),
                request.getMembershipTier(),
                request.getPaymentReference(),
                request.getPaymentAmount()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Application submitted successfully and is awaiting review.")
                        .data(Map.of(
                                "applicationId", application.getId(),
                                "email", application.getEmail(),
                                "tier", application.getMembershipTier().name()
                        ))
                        .build());
    }

    // --- Request DTOs ---

    @Data
    public static class SendOtpRequest {
        @NotBlank @Email
        private String email;
    }

    @Data
    public static class VerifyOtpRequest {
        @NotBlank @Email
        private String email;

        @NotBlank
        private String otpCode;

        @NotBlank
        private String membershipTier;
    }

    @Data
    public static class CreateApplicationRequest {
        @NotBlank private String firstName;
        @NotBlank private String lastName;
        @NotBlank @Email private String email;
        @NotBlank private String addressLine1;
        private String addressLine2;
        @NotBlank private String city;
        @NotBlank private String stateProvince;
        @NotBlank private String zipPostalCode;
        @NotBlank private String country;
        private String phone;
        // Professional fields — collected during application
        private String linkedInProfile;
        private String socials;
        @NotBlank(message = "A professional bio is required")
        private String bio;
        @NotBlank(message = "Please tell us why you want to join GNS")
        private String reasonForJoining;
        @NotBlank(message = "Sectors are required")
        private String sectors;
        @NotBlank(message = "Speech types are required")
        private String speechTypes;
        @NotBlank(message = "Languages are required")
        private String languages;
        @NotBlank @Pattern(regexp = "AFFILIATE|ASSOCIATE|MEMBER")
        private String membershipTier;
        @NotBlank private String paymentReference;
        private int paymentAmount;
    }
}