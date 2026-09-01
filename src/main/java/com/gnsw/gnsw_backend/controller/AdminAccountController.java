package com.gnsw.gnsw_backend.controller;

import com.gnsw.gnsw_backend.dto.response.ApiResponse;
import com.gnsw.gnsw_backend.entity.AdminUser;
import com.gnsw.gnsw_backend.repository.AdminUserRepository;
import com.gnsw.gnsw_backend.security.AdminPermissions;
import com.gnsw.gnsw_backend.service.AdminUserManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.constraints.Pattern;

/**
 * Self-service account endpoints available to every admin-console identity
 * (including MANAGER): view your own profile and change your own password.
 */
@RestController
@RequestMapping("/api/v1/admin/account")
@RequiredArgsConstructor
public class AdminAccountController {

    private final AdminUserRepository adminUserRepository;
    private final AdminUserManagementService adminUserManagementService;

    @PreAuthorize("hasAuthority('ADMIN_IDENTITY')")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAccount(Authentication authentication) {
        AdminUser me = resolve(authentication);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", me.getId());
        data.put("email", me.getEmail());
        data.put("displayName", me.getDisplayName());
        data.put("role", me.getRole().name());
        data.put("status", me.getStatus().name());
        data.put("theme", me.getTheme() != null ? me.getTheme() : "LIGHT");
        data.put("allowedModules", new ArrayList<>(AdminPermissions.allowedModules(me)));
        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Account retrieved.")
                        .data(data)
                        .build());
    }

    @PreAuthorize("hasAuthority('ADMIN_IDENTITY')")
    @PostMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changeOwnPassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {
        AdminUser me = resolve(authentication);
        adminUserManagementService.changeOwnPassword(me,
                request.getCurrentPassword(), request.getNewPassword(),
                request.getNewPasswordConfirmation());
        return ResponseEntity.ok()
                .body(ApiResponse.<Void>builder()
                        .success(true)
                        .message("Password updated successfully.")
                        .build());
    }

    @PreAuthorize("hasAuthority('ADMIN_IDENTITY')")
    @GetMapping("/theme")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTheme(Authentication authentication) {
        AdminUser me = resolve(authentication);
        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Theme retrieved.")
                        .data(Map.of("theme", me.getTheme() != null ? me.getTheme() : "LIGHT"))
                        .build());
    }

    @PreAuthorize("hasAuthority('ADMIN_IDENTITY')")
    @PatchMapping("/theme")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateTheme(
            @RequestBody ThemeRequest request,
            Authentication authentication) {
        AdminUser me = resolve(authentication);
        me.setTheme(request.getTheme());
        adminUserRepository.save(me);
        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Theme updated.")
                        .data(Map.of("theme", me.getTheme()))
                        .build());
    }

    private AdminUser resolve(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof AdminUser adminUser) {
            return adminUser;
        }
        return adminUserRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Admin account not found."));
    }

    @Data
    public static class ThemeRequest {
        @NotBlank(message = "Theme is required")
        @Pattern(regexp = "LIGHT|DARK", message = "Theme must be LIGHT or DARK")
        private String theme;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;

        @NotBlank(message = "New password is required")
        @Size(min = 8, message = "New password must be at least 8 characters")
        private String newPassword;

        @NotBlank(message = "Password confirmation is required")
        private String newPasswordConfirmation;
    }
}