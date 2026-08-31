package com.gnsw.gnsw_backend.controller;

import com.gnsw.gnsw_backend.dto.response.ApiResponse;
import com.gnsw.gnsw_backend.service.NewApplicationService;
import com.gnsw.gnsw_backend.service.OtpService;
import com.gnsw.gnsw_backend.util.EmailUtil;
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
 * Step 3: Submit application (free) → PENDING review
 * The annual membership subscription is charged on the members portal AFTER
 * the applicant is accepted (persistent pay-wall until first dues are paid).
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

        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Email verified successfully. You can now submit your application.")
                        .data(Map.of(
                                "email", email,
                                "tier", request.getMembershipTier()
                        ))
                        .build());
    }

    /**
     * Create application (free, no payment required at submission).
     * The annual membership subscription is charged only AFTER the applicant is
     * accepted and completes their account setup (see the members portal pay-wall).
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
                request.getSocialMediaPlatform(),
                request.getBio(),
                request.getReasonForJoining(),
                request.getCurrentProfessionalRole(),
                request.getFavouriteOrator(),
                request.getSpeechwritingTraining(),
                request.getTrainingDetails(),
                request.getHighestQualification(),
                request.getCurrentJobTitle(),
                request.getCurrentOrganization(),
                request.getMembershipTier()
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
        private String socialMediaPlatform;
        @NotBlank(message = "A bio is required")
        private String bio;
        @NotBlank(message = "Please tell us why you want to join GNSW")
        private String reasonForJoining;
        @NotBlank(message = "Please select your current professional role")
        private String currentProfessionalRole;
        @NotBlank(message = "Please tell us your favourite orator")
        private String favouriteOrator;
        private String speechwritingTraining;
        private String trainingDetails;
        private String highestQualification;
        private String currentJobTitle;
        private String currentOrganization;
        @NotBlank @Pattern(regexp = "AFFILIATE|ASSOCIATE|MEMBER")
        private String membershipTier;
    }
}