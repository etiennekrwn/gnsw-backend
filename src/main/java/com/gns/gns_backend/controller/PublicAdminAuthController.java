package com.gns.gns_backend.controller;

import com.gns.gns_backend.dto.response.ApiResponse;
import com.gns.gns_backend.service.AdminAuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Public admin-console authentication. Separate from the member/public login:
 * these validate only against the admin_users table.
 */
@RestController
@RequestMapping("/api/v1/public/admin/auth")
@RequiredArgsConstructor
public class PublicAdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(@Valid @RequestBody LoginRequest request) {
        Map<String, Object> result = adminAuthService.login(request.getEmail(), request.getPassword());
        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Login successful.")
                        .data(result)
                        .build());
    }

    @GetMapping("/validate-invite")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateInvite(@RequestParam String token) {
        Map<String, Object> result = adminAuthService.validateInvite(token);
        boolean valid = Boolean.TRUE.equals(result.get("valid"));
        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(valid)
                        .message((String) result.get("message"))
                        .data(result)
                        .build());
    }

    /** POST /accept-invite */
    @PostMapping("/accept-invite")
    public ResponseEntity<ApiResponse<Void>> acceptInvite(@Valid @RequestBody AcceptInviteRequest request) {
        adminAuthService.acceptInvite(request.getToken(), request.getEmail(),
                request.getPassword(), request.getPasswordConfirmation());
        return ResponseEntity.ok()
                .body(ApiResponse.<Void>builder()
                        .success(true)
                        .message("Account activated. You can now sign in.")
                        .build());
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    public static class AcceptInviteRequest {
        @NotBlank(message = "Invite token is required")
        private String token;

        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        private String password;

        @NotBlank(message = "Password confirmation is required")
        private String passwordConfirmation;
    }
}