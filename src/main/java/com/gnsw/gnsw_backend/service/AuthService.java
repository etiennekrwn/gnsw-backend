package com.gnsw.gnsw_backend.service;

import com.gnsw.gnsw_backend.dto.request.LoginRequest;
import com.gnsw.gnsw_backend.dto.request.SetPasswordRequest;
import com.gnsw.gnsw_backend.entity.Application;
import com.gnsw.gnsw_backend.entity.Member;
import com.gnsw.gnsw_backend.entity.User;
import com.gnsw.gnsw_backend.enums.ApplicationStatus;
import com.gnsw.gnsw_backend.enums.UserStatus;
import com.gnsw.gnsw_backend.repository.ApplicationRepository;
import com.gnsw.gnsw_backend.repository.MemberRepository;
import com.gnsw.gnsw_backend.repository.UserRepository;
import com.gnsw.gnsw_backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    @Value("${app.frontend.main-site-url:http://localhost:5173}")
    private String mainSiteUrl;

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
                    "message", "This activation link is not valid. Please contact the GNS admin for assistance."
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
                    "message", "This activation link has expired. Please contact the GNS admin for a new link."
            );
        }

        return Map.of(
                "valid", true,
                "status", "VALID",
                "email", user.getEmail() != null ? user.getEmail() : "",
                "message", "Token is valid."
        );
    }

    /**
     * Resends an activation link if the email belongs to an accepted member
     * who has not yet set a password. Returns a structured response so the
     * frontend can show the right message + action based on the person's state.
     *
     * Statuses:
     *  - ACTIVATION_SENT: a fresh link was emailed
     *  - ALREADY_ACTIVE:  member has set a password — direct to login
     *  - PENDING:         application still under review — no email
     *  - NOT_A_MEMBER:    rejected applicant or no record — link to membership page
     */
    public Map<String, Object> resendActivationLink(String email) {
        if (email == null || email.isBlank()) {
            return Map.of(
                    "status", "ACTIVATION_SENT",
                    "message", "If an account is pending activation, a new link has been sent to your email."
            );
        }

        String normalized = email.trim().toLowerCase();

        // Case 1 & 2: check users table first
        User user = userRepository.findByEmail(normalized).orElse(null);
        if (user != null) {
            // Active member — password already set
            if (user.getPasswordSetAt() != null || user.getPasswordHash() != null) {
                return Map.of(
                        "status", "ALREADY_ACTIVE",
                        "message", "You're already an active member — sign in below."
                );
            }

            // Accepted member who hasn't set a password — issue a fresh link
            if (user.getStatus() == UserStatus.ACCEPTED) {
                String token = UUID.randomUUID().toString();
                user.setPasswordSetToken(token);
                user.setPasswordSetTokenExpiresAt(LocalDateTime.now().plusHours(48));
                userRepository.save(user);

                try {
                    emailService.sendApprovalEmail(user.getEmail(), user.getFirstName(),
                            user.getTier() != null ? user.getTier().name() : "MEMBER",
                            user.getProfessionalId(), token, null);
                } catch (Exception e) {
                    // Email failure should never expose whether an account exists.
                }
            }

            return Map.of(
                    "status", "ACTIVATION_SENT",
                    "message", "If an account is pending activation, a new link has been sent to your email."
            );
        }

        // Case 3 & 4: check applications table
        Application application = applicationRepository.findByEmail(normalized).orElse(null);
        if (application != null) {
            if (application.getStatus() == ApplicationStatus.PENDING) {
                return Map.of(
                        "status", "PENDING",
                        "message", "Your membership application is still under review. You have not been accepted as a member yet. You'll receive an email once your application is approved."
                );
            }
            // Rejected applicant — same message as non-member
            return Map.of(
                    "status", "NOT_A_MEMBER",
                    "message", "You are not a member yet.",
                    "membershipUrl", mainSiteUrl + "/membership"
            );
        }

        // Case 5: no record at all
        return Map.of(
                "status", "NOT_A_MEMBER",
                "message", "You are not a member yet.",
                "membershipUrl", mainSiteUrl + "/membership"
        );
    }

    /**
     * Requests a password reset link. Always returns a generic success response
     * so we never reveal whether an email has an account (prevents enumeration).
     */
    public void forgotPassword(String email) {
        if (email == null || email.isBlank()) {
            return;
        }

        String normalized = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalized).orElse(null);

        // Only send to accepted members that have actually set a password.
        if (user != null && user.getStatus() == UserStatus.ACCEPTED
                && user.getPasswordHash() != null) {
            String token = UUID.randomUUID().toString();
            user.setPasswordResetToken(token);
            user.setPasswordResetTokenExpiresAt(LocalDateTime.now().plusHours(1));
            userRepository.save(user);

            try {
                emailService.sendPasswordResetEmail(user.getEmail(), user.getFirstName(), token);
            } catch (Exception e) {
                // Swallow — we deliberately return a generic response either way.
            }
        }
    }

    /**
     * Resets a member's password using a valid, unexpired reset token.
     */
    public void resetPassword(String token, String newPassword, String confirmation) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Reset token is required.");
        }

        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token."));

        if (user.getPasswordResetTokenExpiresAt() == null ||
                user.getPasswordResetTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("This reset link has expired. Please request a new one.");
        }

        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters.");
        }
        if (!newPassword.equals(confirmation)) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiresAt(null);
        userRepository.save(user);
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

        // Check onboarding status — has the user uploaded a profile photo?
        Member member = memberRepository.findByUserId(user.getId()).orElse(null);
        boolean onboardingCompleted = member != null && member.getProfileImageUrl() != null
                && !member.getProfileImageUrl().isBlank();
        userMap.put("onboardingCompleted", onboardingCompleted);

        return Map.of(
                "accessToken", token,
                "tokenType", "Bearer",
                "expiresIn", 86400,
                "user", userMap
        );
    }
}