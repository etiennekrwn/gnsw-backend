package com.gnsw.gnsw_backend.controller;

import com.gnsw.gnsw_backend.dto.response.ApiResponse;
import com.gnsw.gnsw_backend.entity.Member;
import com.gnsw.gnsw_backend.entity.MemberPreference;
import com.gnsw.gnsw_backend.entity.MemberSubscription;
import com.gnsw.gnsw_backend.entity.User;
import com.gnsw.gnsw_backend.enums.MembershipTier;
import com.gnsw.gnsw_backend.repository.MemberPreferenceRepository;
import com.gnsw.gnsw_backend.repository.MemberRepository;
import com.gnsw.gnsw_backend.repository.MemberSubscriptionRepository;
import com.gnsw.gnsw_backend.repository.UserRepository;
import com.gnsw.gnsw_backend.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final MemberSubscriptionRepository memberSubscriptionRepository;
    private final MemberPreferenceRepository memberPreferenceRepository;
    private final PaymentService paymentService;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Value("${app.subscription.grace-days:5}")
    private int graceDays;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(Authentication authentication) {
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        Member member = memberRepository.findByUserId(user.getId()).orElse(null);

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("email", user.getEmail());
        profile.put("firstName", user.getFirstName());
        profile.put("lastName", user.getLastName());
        profile.put("tier", user.getTier() != null ? user.getTier().name() : null);
        profile.put("role", user.getRole());
        profile.put("professionalId", user.getProfessionalId());
        profile.put("status", user.getStatus() != null ? user.getStatus().name() : null);

        if (member != null) {
            profile.put("profileImageUrl", member.getProfileImageUrl());
            profile.put("organisation", member.getOrganisation());
            profile.put("bio", member.getBio());
            profile.put("phone", member.getPhone());
            profile.put("sectors", member.getSectors());
            profile.put("speechTypes", member.getSpeechTypes());
            profile.put("languages", member.getLanguages());
            profile.put("zone", member.getZone());
            profile.put("linkedInProfile", member.getLinkedInProfile());
            profile.put("socials", member.getSocials());
            profile.put("reasonForJoining", member.getReasonForJoining());
            profile.put("isVerified", member.isVerified());
            profile.put("memberSince", member.getCreatedAt());
        }

        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Profile retrieved.")
                        .data(profile)
                        .build());
    }

    @PostMapping("/onboarding/photo")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadOnboardingPhoto(
            @Valid @RequestBody OnboardingPhotoRequest request,
            Authentication authentication) {
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        Member member = memberRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Member newMember = Member.builder()
                            .userId(user.getId())
                            .build();
                    return memberRepository.save(newMember);
                });

        member.setProfileImageUrl(request.getProfileImage());
        memberRepository.save(member);

        Map<String, Object> result = Map.of(
                "profileImageUrl", member.getProfileImageUrl(),
                "onboardingCompleted", true
        );

        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Profile photo saved successfully.")
                        .data(result)
                        .build());
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        Member member = memberRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Member newMember = Member.builder()
                            .userId(user.getId())
                            .build();
                    return memberRepository.save(newMember);
                });

        // Update member fields
        if (request.getOrganisation() != null) {
            member.setOrganisation(request.getOrganisation());
        }
        if (request.getBio() != null) {
            member.setBio(request.getBio());
        }
        if (request.getPhone() != null) {
            member.setPhone(request.getPhone());
        }
        if (request.getSectors() != null) {
            member.setSectors(request.getSectors());
        }
        if (request.getSpeechTypes() != null) {
            member.setSpeechTypes(request.getSpeechTypes());
        }
        if (request.getLanguages() != null) {
            member.setLanguages(request.getLanguages());
        }
        if (request.getZone() != null) {
            member.setZone(request.getZone());
        }
        if (request.getProfileImageUrl() != null) {
            member.setProfileImageUrl(request.getProfileImageUrl());
        }
        if (request.getLinkedInProfile() != null) {
            member.setLinkedInProfile(request.getLinkedInProfile());
        }
        if (request.getSocials() != null) {
            member.setSocials(request.getSocials());
        }

        memberRepository.save(member);

        // Build response
        Map<String, Object> profile = new HashMap<>();
        profile.put("organisation", member.getOrganisation());
        profile.put("bio", member.getBio());
        profile.put("phone", member.getPhone());
        profile.put("sectors", member.getSectors());
        profile.put("speechTypes", member.getSpeechTypes());
        profile.put("languages", member.getLanguages());
        profile.put("zone", member.getZone());
        profile.put("profileImageUrl", member.getProfileImageUrl());
        profile.put("linkedInProfile", member.getLinkedInProfile());
        profile.put("socials", member.getSocials());

        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Profile updated successfully.")
                        .data(profile)
                        .build());
    }

    @GetMapping("/subscription")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSubscription(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        MemberSubscription sub = memberSubscriptionRepository.findByUserId(user.getId()).orElse(null);

        Map<String, Object> result = new HashMap<>();
        result.put("status", sub != null ? sub.getStatus() : null);
        result.put("planCode", sub != null ? sub.getPlanCode() : null);
        result.put("subscriptionCode", sub != null ? sub.getSubscriptionCode() : null);
        result.put("nextPaymentDate", sub != null ? sub.getNextPaymentDate() : null);
        result.put("graceStartedAt", sub != null ? sub.getGraceStartedAt() : null);
        result.put("lastPaymentAttemptAt", sub != null ? sub.getLastPaymentAttemptAt() : null);
        result.put("graceDays", graceDays);
        result.put("graceEndsAt", sub != null && sub.getGraceStartedAt() != null
                ? sub.getGraceStartedAt().plusDays(graceDays) : null);
        result.put("isActive", subscriptionHasAccess(sub));
        result.put("tier", user.getTier() != null ? user.getTier().name() : null);
        result.put("paymentDue", sub != null && "payment_due".equalsIgnoreCase(sub.getStatus()));
        result.put("annualFee", user.getTier() != null
                ? com.gnsw.gnsw_backend.util.MembershipFees.annualFeeLabel(user.getTier().name()) : null);

        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Subscription retrieved.")
                        .data(result)
                        .build());
    }

    /**
     * Initialize the first membership-dues payment (pay-wall). Only valid for an
     * accepted, authenticated member whose subscription is still "payment_due".
     */
    @PostMapping("/dues/init")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initializeDues(Authentication authentication) {
        User user = resolveUser(authentication);
        Map<String, Object> result = paymentService.initializeDues(user.getId());
        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Payment initialized.")
                        .data(result)
                        .build());
    }

    /**
     * Finalize the first-dues payment. On success the subscription is set to
     * "active" (with auto-renew) and the member passes the pay-wall.
     */
    @PostMapping("/dues/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> finalizeDues(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        Object refObj = body.get("reference");
        if (!(refObj instanceof String reference) || reference.isBlank()) {
            throw new IllegalArgumentException("Payment reference is required.");
        }
        User user = resolveUser(authentication);
        Map<String, Object> result = paymentService.finalizeDues(user.getId(), reference);
        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success("SUCCESS".equals(result.get("status")))
                        .message((String) result.get("message"))
                        .data(result)
                        .build());
    }

    /**
     * Upgrades the member's plan (AFFILIATE -> ASSOCIATE -> MEMBER). FELLOW is
     * admin-assigned only and not available here. Paystack's plan change applies
     * on the next invoice (no automatic mid-cycle proration).
     */
    @PostMapping("/subscription/upgrade")
    public ResponseEntity<ApiResponse<Map<String, Object>>> upgradeSubscription(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        Object newTierObj = body.get("newTier");
        if (!(newTierObj instanceof String newTier) || newTier.isBlank()) {
            throw new IllegalArgumentException("A target tier (newTier) is required.");
        }
        MembershipTier current = user.getTier();
        MembershipTier target;
        try {
            target = MembershipTier.valueOf(newTier.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown tier: " + newTier);
        }

        int currentRank = tierRank(current);
        int targetRank = tierRank(target);
        if (current == null || currentRank >= targetRank) {
            throw new IllegalArgumentException("You can only upgrade to a higher tier (Affiliate to Associate to Member).");
        }
        if (target == MembershipTier.FELLOW) {
            throw new IllegalArgumentException("The Fellow tier is assigned by the admin and cannot be purchased.");
        }

        MemberSubscription sub = memberSubscriptionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("No subscription found for this account."));

        // Change the Paystack plan, then update our records.
        String newPlanCode = paymentService.changeSubscriptionPlan(sub.getSubscriptionCode(), target.name());
        sub.setPlanCode(newPlanCode);
        memberSubscriptionRepository.save(sub);

        user.setTier(target);
        userRepository.save(user);

        Map<String, Object> result = new HashMap<>();
        result.put("tier", target.name());
        result.put("planCode", newPlanCode);
        result.put("message", "Your tier has been upgraded to " + target.name()
                + ". The new plan amount will apply on your next renewal.");

        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Tier upgraded successfully.")
                        .data(result)
                        .build());
    }

    private int tierRank(MembershipTier tier) {
        if (tier == null) return 0;
        return switch (tier) {
            case AFFILIATE -> 1;
            case ASSOCIATE -> 2;
            case MEMBER -> 3;
            case FELLOW -> 4;
        };
    }

    private boolean subscriptionHasAccess(MemberSubscription sub) {
        if (sub == null) return false;
        String status = sub.getStatus() == null ? "" : sub.getStatus().toLowerCase();
        if ("active".equals(status) || "pending".equals(status)) return true;
        if ("past_due".equals(status)) {
            // Access is kept through the grace window (graceDays from the first failure).
            return sub.getGraceStartedAt() == null
                    || sub.getGraceStartedAt().plusDays(graceDays).isAfter(LocalDateTime.now());
        }
        if ("cancelled".equals(status)) {
            // Access lasts until the end of the paid year.
            return sub.getNextPaymentDate() != null && sub.getNextPaymentDate().isAfter(LocalDateTime.now());
        }
        return false;
    }

    @PostMapping("/subscription/cancel")
    public ResponseEntity<ApiResponse<Map<String, Object>>> cancelSubscription(Authentication authentication) {
        String username = authentication.getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        MemberSubscription sub = memberSubscriptionRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("No subscription found for this account."));

        if ("cancelled".equalsIgnoreCase(sub.getStatus())) {
            throw new IllegalArgumentException("Your subscription has already been cancelled.");
        }

        // Cancel the Paystack subscription (if any) so no further charges occur.
        paymentService.cancelSubscription(sub.getSubscriptionCode());
        sub.setStatus("cancelled");
        memberSubscriptionRepository.save(sub);

        Map<String, Object> result = new HashMap<>();
        result.put("status", sub.getStatus());
        result.put("nextPaymentDate", sub.getNextPaymentDate());

        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Subscription cancelled. You will not be charged again. Your access continues until the end of your paid period.")
                        .data(result)
                        .build());
    }

    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPreferences(Authentication authentication) {
        User user = resolveUser(authentication);
        MemberPreference p = memberPreferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> defaultPreferences(user.getId()));
        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Preferences retrieved.")
                        .data(prefMap(p))
                        .build());
    }

    @PatchMapping("/preferences")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updatePreferences(
            @RequestBody Map<String, Object> body,
            Authentication authentication) {
        User user = resolveUser(authentication);
        MemberPreference p = memberPreferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> defaultPreferences(user.getId()));

        applyBoolean(body, "emailNotifications", p::setEmailNotifications);
        applyBoolean(body, "weeklyDigest", p::setWeeklyDigest);
        applyBoolean(body, "newArticleAlerts", p::setNewArticleAlerts);
        applyBoolean(body, "commentAlerts", p::setCommentAlerts);
        applyBoolean(body, "memberAnnouncements", p::setMemberAnnouncements);
        applyBoolean(body, "darkMode", p::setDarkMode);
        applyString(body, "fontSize", p::setFontSize);
        memberPreferenceRepository.save(p);

        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Preferences updated.")
                        .data(prefMap(p))
                        .build());
    }

    @PostMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody PasswordChangeRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect.");
        }
        if (!request.getNewPassword().equals(request.getNewPasswordConfirmation())) {
            throw new IllegalArgumentException("New passwords do not match.");
        }
        if (request.getNewPassword().length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters.");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        return ResponseEntity.ok()
                .body(ApiResponse.<Void>builder()
                        .success(true)
                        .message("Password updated successfully.")
                        .build());
    }

    @PostMapping("/account/deletion-request")
    public ResponseEntity<ApiResponse<Void>> deletionRequest(
            @Valid @RequestBody DeletionRequest request,
            Authentication authentication) {
        User user = resolveUser(authentication);
        if (user.getPasswordHash() == null
                || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Password is incorrect.");
        }
        user.setDeletionRequestedAt(LocalDateTime.now());
        userRepository.save(user);
        return ResponseEntity.ok()
                .body(ApiResponse.<Void>builder()
                        .success(true)
                        .message("Deletion request received. Our team will review it shortly.")
                        .build());
    }

    private User resolveUser(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
    }

    private MemberPreference defaultPreferences(java.util.UUID userId) {
        return MemberPreference.builder()
                .userId(userId)
                .emailNotifications(true)
                .weeklyDigest(false)
                .newArticleAlerts(true)
                .commentAlerts(true)
                .memberAnnouncements(false)
                .darkMode(false)
                .fontSize("medium")
                .build();
    }

    private Map<String, Object> prefMap(MemberPreference p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("emailNotifications", p.getEmailNotifications());
        m.put("weeklyDigest", p.getWeeklyDigest());
        m.put("newArticleAlerts", p.getNewArticleAlerts());
        m.put("commentAlerts", p.getCommentAlerts());
        m.put("memberAnnouncements", p.getMemberAnnouncements());
        m.put("darkMode", p.getDarkMode());
        m.put("fontSize", p.getFontSize());
        return m;
    }

    private void applyBoolean(Map<String, Object> body, String key, java.util.function.Consumer<Boolean> setter) {
        Object v = body.get(key);
        if (v instanceof Boolean b) setter.accept(b);
    }

    private void applyString(Map<String, Object> body, String key, java.util.function.Consumer<String> setter) {
        Object v = body.get(key);
        if (v instanceof String s) setter.accept(s);
    }

    @Data
    public static class OnboardingPhotoRequest {
        @jakarta.validation.constraints.NotBlank(message = "Profile image is required")
        @Size(max = 5000000, message = "Image data must not exceed 5MB")
        private String profileImage;
    }

    @Data
    public static class PasswordChangeRequest {
        @NotBlank(message = "Current password is required")
        private String currentPassword;

        @NotBlank(message = "New password is required")
        private String newPassword;

        @NotBlank(message = "Password confirmation is required")
        private String newPasswordConfirmation;
    }

    @Data
    public static class DeletionRequest {
        @NotBlank(message = "Password is required")
        private String password;
    }

    @Data
    public static class UpdateProfileRequest {
        @Size(max = 255)
        private String organisation;

        @Size(max = 2000)
        private String bio;

        private String phone;

        @Size(max = 1000)
        private String sectors;

        @Size(max = 1000)
        private String speechTypes;

        @Size(max = 500)
        private String languages;

        private String zone;

        private String profileImageUrl;

        @Size(max = 500)
        private String linkedInProfile;

        @Size(max = 2000)
        private String socials;
    }
}