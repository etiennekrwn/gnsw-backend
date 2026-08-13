package com.gnsw.gnsw_backend.service;

import com.gnsw.gnsw_backend.dto.request.LoginRequest;
import com.gnsw.gnsw_backend.dto.request.SetPasswordRequest;
import com.gnsw.gnsw_backend.entity.User;
import com.gnsw.gnsw_backend.enums.UserStatus;
import com.gnsw.gnsw_backend.repository.UserRepository;
import com.gnsw.gnsw_backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    public void setPassword(SetPasswordRequest request) {
        // Find user by token
        User user = userRepository.findByPasswordSetToken(request.getToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired token."));

        // Check token expiry
        if (user.getPasswordSetTokenExpiresAt() == null ||
                user.getPasswordSetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Token has expired. Please contact admin for a new link.");
        }

        // Check if password already set
        if (user.getPasswordSetAt() != null) {
            throw new IllegalArgumentException("Password has already been set. Please log in.");
        }

        // Validate passwords match
        if (!request.getPassword().equals(request.getPasswordConfirmation())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        // Validate username uniqueness
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username is already taken.");
        }

        // Hash password and update user
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPasswordSetAt(LocalDateTime.now());
        user.setPasswordSetToken(null);
        user.setPasswordSetTokenExpiresAt(null);
        // Setting the password proves ownership of the email
        user.setEmailVerifiedAt(LocalDateTime.now());
        userRepository.save(user);

        // Send confirmation email (async)
        emailService.sendPasswordSetConfirmation(user.getEmail(), user.getUsername());
    }

    /**
     * Validates a password-set token WITHOUT consuming it.
     * Used by the member portal to decide whether the activation form
     * should be shown at all.
     *
     * Statuses:
     *  - VALID:   token exists, not expired, password not yet set — form may be shown
     *  - USED:    password was already set — link is permanently dead, direct to login
     *  - EXPIRED: token past its expiry — link is dead, require a new one
     *  - INVALID: no matching token — link is dead
     */
    public Map<String, Object> validateSetPasswordToken(String token) {
        if (token == null || token.isBlank()) {
            return Map.of(
                    "valid", false,
                    "status", "INVALID",
                    "message", "Invalid or missing activation token."
            );
        }

        User user = userRepository.findByPasswordSetToken(token).orElse(null);
        if (user == null) {
            return Map.of(
                    "valid", false,
                    "status", "INVALID",
                    "message", "This activation link is not valid. Please contact the GNSW admin for assistance."
            );
        }

        // Password already set — the link is permanently used up.
        if (user.getPasswordSetAt() != null || user.getPasswordHash() != null) {
            return Map.of(
                    "valid", false,
                    "status", "USED",
                    "email", user.getEmail(),
                    "message", "This activation link has already been used. You can log in directly."
            );
        }

        // Token expired — still usable if never set, but needs a fresh link from admin.
        if (user.getPasswordSetTokenExpiresAt() == null ||
                user.getPasswordSetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            return Map.of(
                    "valid", false,
                    "status", "EXPIRED",
                    "email", user.getEmail(),
                    "message", "This activation link has expired. Please contact the GNSW admin for a new link."
            );
        }

        return Map.of(
                "valid", true,
                "status", "VALID",
                "email", user.getEmail() != null ? user.getEmail() : "",
                "message", "Token is valid."
        );
    }

    public Map<String, Object> login(LoginRequest request) {
        // Try to find user by username first, then by email
        User user = userRepository.findByUsername(request.getUsername())
                .orElseGet(() -> userRepository.findByEmail(request.getUsername())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid username or password.")));

        // Check if account is active
        if (user.getStatus() != UserStatus.ACCEPTED) {
            throw new IllegalArgumentException("Account is not active. Please contact support.");
        }

        // Check if password has been set
        if (user.getPasswordHash() == null) {
            throw new IllegalArgumentException("Account not activated. Please set your password using the link sent to your email.");
        }

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password.");
        }

        // Update last login
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Generate JWT
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole());

        java.util.Map<String, Object> userMap = new java.util.HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("email", user.getEmail());
        userMap.put("firstName", user.getFirstName());
        userMap.put("lastName", user.getLastName());
        userMap.put("tier", user.getTier() != null ? user.getTier().name() : null);
        userMap.put("role", user.getRole());
        userMap.put("professionalId", user.getProfessionalId());

        return Map.of(
                "accessToken", token,
                "tokenType", "Bearer",
                "expiresIn", 86400,
                "user", userMap
        );
    }
}