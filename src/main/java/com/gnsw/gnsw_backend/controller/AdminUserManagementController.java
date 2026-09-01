package com.gnsw.gnsw_backend.controller;

import com.gnsw.gnsw_backend.dto.response.ApiResponse;
import com.gnsw.gnsw_backend.entity.AdminUser;
import com.gnsw.gnsw_backend.repository.AdminUserRepository;
import com.gnsw.gnsw_backend.service.AdminUserManagementService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * SUPER_ADMIN / ADMIN management of admin-console accounts. MANAGER accounts
 * are blocked first in SecurityConfig, and again here for defense in depth.
 */
@RestController
@RequestMapping("/api/v1/admin/users-admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
public class AdminUserManagementController {

    private final AdminUserManagementService adminUserManagementService;
    private final AdminUserRepository adminUserRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> listUsers() {
        List<Map<String, Object>> data = adminUserManagementService.list();
        return ok(data, "Admin accounts retrieved.");
    }

    @PostMapping("/invite")
    public ResponseEntity<ApiResponse<Map<String, Object>>> invite(
            @Valid @RequestBody InviteRequest request,
            Authentication authentication) {
        AdminUser actor = resolve(authentication);
        Map<String, Object> data = adminUserManagementService.invite(actor,
                request.getEmail(), request.getDisplayName(), request.getRole(),
                request.getAllowedModules());
        boolean emailDelivered = Boolean.TRUE.equals(data.get("emailDelivered"));
        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message(emailDelivered
                                ? "Invite sent. The recipient will set their own password."
                                : (String) data.get("emailMessage"))
                        .data(data)
                        .build());
    }

    @PostMapping("/{id}/resend")
    public ResponseEntity<ApiResponse<Void>> resend(@PathVariable UUID id, Authentication authentication) {
        adminUserManagementService.resendInvite(resolve(authentication), id);
        return ok("Invitation resent to the admin's email.");
    }

    @PostMapping("/{id}/revoke")
    public ResponseEntity<ApiResponse<Void>> revoke(@PathVariable UUID id, Authentication authentication) {
        adminUserManagementService.revokeInvite(resolve(authentication), id);
        return ok("Invite link revoked.");
    }

    @PostMapping("/{id}/disable")
    public ResponseEntity<ApiResponse<Void>> disable(@PathVariable UUID id, Authentication authentication) {
        adminUserManagementService.disable(resolve(authentication), id);
        return ok("Account disabled.");
    }

    @PostMapping("/{id}/enable")
    public ResponseEntity<ApiResponse<Void>> enable(@PathVariable UUID id, Authentication authentication) {
        adminUserManagementService.enable(resolve(authentication), id);
        return ok("Account enabled.");
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<ApiResponse<Void>> setRole(@PathVariable UUID id,
                                                     @Valid @RequestBody SetRoleRequest request,
                                                     Authentication authentication) {
        adminUserManagementService.setRole(resolve(authentication), id,
                request.getRole(), request.getAllowedModules());
        return ok("Role updated.");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id, Authentication authentication) {
        adminUserManagementService.delete(resolve(authentication), id);
        return ok("Admin account deleted.");
    }

    private AdminUser resolve(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof AdminUser adminUser) {
            return adminUser;
        }
        return adminUserRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Admin account not found."));
    }

    private ResponseEntity<ApiResponse<Void>> ok(String message) {
        return ResponseEntity.ok()
                .body(ApiResponse.<Void>builder()
                        .success(true)
                        .message(message)
                        .build());
    }

    private <T> ResponseEntity<ApiResponse<T>> ok(T data, String message) {
        return ResponseEntity.ok()
                .body(ApiResponse.<T>builder()
                        .success(true)
                        .message(message)
                        .data(data)
                        .build());
    }

    @Data
    public static class InviteRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Please provide a valid email address")
        private String email;

        @NotBlank(message = "Name is required")
        private String displayName;

        @NotBlank(message = "Role is required")
        @Pattern(regexp = "ADMIN|MANAGER", message = "Role must be ADMIN or MANAGER")
        private String role;

        private Set<String> allowedModules;
    }

    @Data
    public static class SetRoleRequest {
        @NotBlank(message = "Role is required")
        @Pattern(regexp = "ADMIN|MANAGER", message = "Role must be ADMIN or MANAGER")
        private String role;

        private Set<String> allowedModules;
    }
}
