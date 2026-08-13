package com.gnsw.gnsw_backend.controller;

import com.gnsw.gnsw_backend.dto.request.LoginRequest;
import com.gnsw.gnsw_backend.dto.request.SetPasswordRequest;
import com.gnsw.gnsw_backend.dto.response.ApiResponse;
import com.gnsw.gnsw_backend.service.AuthService;
import jakarta.validation.Valid;
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
}