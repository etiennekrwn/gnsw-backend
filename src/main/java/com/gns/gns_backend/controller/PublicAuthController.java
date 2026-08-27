package com.gns.gns_backend.controller;

import com.gns.gns_backend.dto.request.LoginRequest;
import com.gns.gns_backend.dto.request.SetPasswordRequest;
import com.gns.gns_backend.dto.response.ApiResponse;
import com.gns.gns_backend.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public/auth")
@RequiredArgsConstructor
public class PublicAuthController {

    private final AuthService authService;

    @PostMapping("/set-password")
    public ResponseEntity<ApiResponse<Void>> setPassword(
            @Valid @RequestBody SetPasswordRequest request) {
        authService.setPassword(request);
        return ResponseEntity.ok()
                .body(ApiResponse.<Void>builder()
                        .success(true)
                        .message("Account activated successfully. You can now log in.")
                        .build());
    }

    @GetMapping("/set-password/validate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateSetPasswordToken(
            @RequestParam String token) {
        Map<String, Object> result = authService.validateSetPasswordToken(token);
        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success((Boolean) result.get("valid"))
                        .message((String) result.get("message"))
                        .data(result)
                        .build());
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody LoginRequest request) {
        Map<String, Object> result = authService.login(request);
        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Login successful.")
                        .data(result)
                        .build());
    }

    @PostMapping("/resend-activation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resendActivation(
            @Valid @RequestBody ResendActivationRequest request) {
        Map<String, Object> result = authService.resendActivationLink(request.getEmail());
        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message((String) result.get("message"))
                        .data(result)
                        .build());
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok()
                .body(ApiResponse.<Void>builder()
                        .success(true)
                        .message("If an account exists for that email, a password reset link has been sent.")
                        .build());
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getPassword(), request.getPasswordConfirmation());
        return ResponseEntity.ok()
                .body(ApiResponse.<Void>builder()
                        .success(true)
                        .message("Password reset successfully. You can now log in.")
                        .build());
    }

    @Data
    public static class ResendActivationRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        private String email;
    }

    @Data
    public static class ForgotPasswordRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        private String email;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank(message = "Token is required")
        private String token;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        @NotBlank(message = "Password confirmation is required")
        private String passwordConfirmation;
    }
}