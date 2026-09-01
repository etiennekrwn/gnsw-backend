package com.gnsw.gnsw_backend.controller;

import com.gnsw.gnsw_backend.dto.response.ApiResponse;
import com.gnsw.gnsw_backend.entity.AdminUser;
import com.gnsw.gnsw_backend.entity.Application;
import com.gnsw.gnsw_backend.entity.Member;
import com.gnsw.gnsw_backend.entity.MemberSubscription;
import com.gnsw.gnsw_backend.entity.Payment;
import com.gnsw.gnsw_backend.entity.User;
import com.gnsw.gnsw_backend.enums.ApplicationStatus;
import com.gnsw.gnsw_backend.enums.MembershipTier;
import com.gnsw.gnsw_backend.enums.PaymentStatus;
import com.gnsw.gnsw_backend.enums.UserStatus;
import com.gnsw.gnsw_backend.repository.AdminUserRepository;
import com.gnsw.gnsw_backend.repository.MemberRepository;
import com.gnsw.gnsw_backend.repository.MemberSubscriptionRepository;
import com.gnsw.gnsw_backend.repository.PaymentRepository;
import com.gnsw.gnsw_backend.repository.UserRepository;
import com.gnsw.gnsw_backend.service.EmailService;
import com.gnsw.gnsw_backend.service.NewApplicationService;
import com.gnsw.gnsw_backend.service.PaymentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class NewAdminController {

    private final NewApplicationService newApplicationService;
    private final UserRepository userRepository;
    private final AdminUserRepository adminUserRepository;
    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final EmailService emailService;
    private final PaymentService paymentService;
    private final MemberSubscriptionRepository memberSubscriptionRepository;

    @GetMapping("/applications")
    public ResponseEntity<ApiResponse<Page<Application>>> getApplications(
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String order) {
        PageRequest pageable = buildPageRequest(page, size, sort, order);
        Page<Application> applications;
        if ("ALL".equalsIgnoreCase(status)) {
            applications = newApplicationService.getAllApplications(pageable);
        } else {
            ApplicationStatus appStatus;
            try {
                appStatus = ApplicationStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Invalid status filter: " + status);
            }
            applications = newApplicationService.getApplications(appStatus, pageable);
        }
        return ResponseEntity.ok()
                .body(ApiResponse.<Page<Application>>builder()
                        .success(true)
                        .message("Applications retrieved.")
                        .data(applications)
                        .build());
    }

    @GetMapping("/applications/{id}")
    public ResponseEntity<ApiResponse<Application>> getApplicationDetail(@PathVariable UUID id) {
        Application application = newApplicationService.getApplication(id);
        enrichApplicationPayment(application);
        return ResponseEntity.ok()
                .body(ApiResponse.<Application>builder()
                        .success(true)
                        .message("Application details retrieved.")
                        .data(application)
                        .build());
    }

    /**
     * For an APPROVED application, populate the payment status + reference from the
     * member's real payment/subscription record so the admin detail page can show
     * whether the accepted member has paid their annual dues and their payment reference.
     * Pending and rejected applications are left untouched (no payment happens until approval).
     */
    private void enrichApplicationPayment(Application application) {
        if (application == null || application.getStatus() != ApplicationStatus.APPROVED) {
            return;
        }

        User user = userRepository.findByEmail(application.getEmail()).orElse(null);
        if (user == null) {
            // Accepted application with no user record yet — treat as not yet paid.
            application.setPaymentStatus("PAYMENT_DUE");
            return;
        }

        MemberSubscription subscription = memberSubscriptionRepository.findByUserId(user.getId()).orElse(null);
        boolean paid = subscription != null && "active".equalsIgnoreCase(subscription.getStatus());

        // Latest successful payment, if any.
        List<Payment> successful = paymentRepository.findAllByUserIdAndStatus(user.getId(), PaymentStatus.SUCCESS);
        String reference = null;
        if (!successful.isEmpty()) {
            reference = successful.stream()
                    .max(Comparator.comparing(Payment::getPaidAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                    .map(Payment::getReference)
                    .orElse(null);
        }
        if (reference == null && subscription != null && subscription.getSubscriptionCode() != null
                && !subscription.getSubscriptionCode().isBlank()) {
            reference = subscription.getSubscriptionCode();
        }

        application.setPaymentStatus(paid ? "ACTIVE" : "PAYMENT_DUE");
        application.setPaymentReference(reference);
    }

    @PostMapping("/applications/{id}/approve")
    public ResponseEntity<ApiResponse<Map<String, String>>> approveApplication(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) ApproveRequest request,
            Authentication authentication) {
        UUID adminId = getAdminId(authentication);
        String customMessage = request != null ? request.getCustomMessage() : null;
        String professionalId = newApplicationService.approveApplication(id, adminId, customMessage);
        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(true)
                        .message("Application approved. User + Member records created. Welcome email sent.")
                        .data(Map.of("professionalId", professionalId))
                        .build());
    }

    @PostMapping("/applications/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectApplication(
            @PathVariable UUID id,
            @Valid @RequestBody RejectRequest request,
            Authentication authentication) {
        UUID adminId = getAdminId(authentication);
        newApplicationService.rejectApplication(id, adminId, request.getReason());
        return ResponseEntity.ok()
                .body(ApiResponse.<Void>builder()
                        .success(true)
                        .message("Application rejected. Notification sent.")
                        .build());
    }

    @GetMapping("/members")
    public ResponseEntity<ApiResponse<Page<Map<String, Object>>>> getMembers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String order) {
        PageRequest pageable = buildPageRequest(page, size, sort, order);
        Page<User> members = userRepository.findByStatus(UserStatus.ACCEPTED, pageable);
        Page<Map<String, Object>> mapped = members.map(user -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", user.getId());
            m.put("email", user.getEmail());
            m.put("firstName", user.getFirstName());
            m.put("lastName", user.getLastName());
            m.put("tier", user.getTier() != null ? user.getTier().name() : null);
            m.put("professionalId", user.getProfessionalId());
            m.put("status", user.getStatus() != null ? user.getStatus().name() : null);
            m.put("city", user.getCity());
            m.put("stateProvince", user.getStateProvince());
            m.put("country", user.getCountry());
            m.put("approvedAt", user.getApprovedAt());
            m.put("lastLoginAt", user.getLastLoginAt());
            m.put("subscriptionStatus", subscriptionStatusLabel(user.getId()));
            return m;
        });
        return ResponseEntity.ok()
                .body(ApiResponse.<Page<Map<String, Object>>>builder()
                        .success(true)
                        .message("Members retrieved.")
                        .data(mapped)
                        .build());
    }

    @GetMapping("/members/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMemberDetail(@PathVariable UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        Member member = memberRepository.findByUserId(user.getId()).orElse(null);

        Map<String, Object> detail = new HashMap<>();
        detail.put("id", user.getId());
        detail.put("email", user.getEmail());
        detail.put("firstName", user.getFirstName());
        detail.put("lastName", user.getLastName());
        detail.put("tier", user.getTier() != null ? user.getTier().name() : null);
        detail.put("professionalId", user.getProfessionalId());
        detail.put("status", user.getStatus() != null ? user.getStatus().name() : null);
        detail.put("city", user.getCity());
        detail.put("stateProvince", user.getStateProvince());
        detail.put("country", user.getCountry());
        detail.put("approvedAt", user.getApprovedAt());
        detail.put("lastLoginAt", user.getLastLoginAt());
        detail.put("emailVerifiedAt", user.getEmailVerifiedAt());
        detail.put("subscriptionStatus", subscriptionStatusLabel(user.getId()));
        if (member != null) {
            detail.put("organisation", member.getOrganisation());
            detail.put("bio", member.getBio());
            detail.put("phone", member.getPhone());
            detail.put("currentProfessionalRole", member.getCurrentProfessionalRole());
            detail.put("favouriteOrator", member.getFavouriteOrator());
            detail.put("speechwritingTraining", member.getSpeechwritingTraining());
            detail.put("trainingDetails", member.getTrainingDetails());
            detail.put("highestQualification", member.getHighestQualification());
            detail.put("currentJobTitle", member.getCurrentJobTitle());
            detail.put("currentOrganization", member.getCurrentOrganization());
            detail.put("zone", member.getZone());
            detail.put("socialMediaPlatform", member.getSocialMediaPlatform());
            detail.put("reasonForJoining", member.getReasonForJoining());
            detail.put("isVerified", member.isVerified());
        }
        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Member details retrieved.")
                        .data(detail)
                        .build());
    }

@PutMapping("/members/{id}/tier")
    public ResponseEntity<ApiResponse<Map<String, Object>>> promoteToFellow(
            @PathVariable UUID id, Authentication authentication) {
        // Guard: only SUPER_ADMIN or ADMIN may promote to Fellow (never a MANAGER).
        Object principal = authentication.getPrincipal();
        if (principal instanceof AdminUser admin) {
            if (admin.getRole() != com.gnsw.gnsw_backend.enums.AdminRole.SUPER_ADMIN
                    && admin.getRole() != com.gnsw.gnsw_backend.enums.AdminRole.ADMIN) {
                throw new IllegalArgumentException("Only Super Admin or Admin can promote a member to Fellow.");
            }
        } else {
            throw new IllegalArgumentException("Only an admin account can promote to Fellow.");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        if (user.getTier() == MembershipTier.FELLOW) {
            throw new IllegalArgumentException("This member is already a Fellow.");
        }

        // Promote to FELLOW (permanent, lifetime membership — no further dues).
        user.setTier(MembershipTier.FELLOW);
        userRepository.save(user);

        // Fellows no longer pay: cancel any active Paystack subscription and mark it
        // as a lifetime "fellow" membership so they are never re-billed.
        memberSubscriptionRepository.findByUserId(user.getId()).ifPresent(sub -> {
            paymentService.cancelSubscription(sub.getSubscriptionCode());
            sub.setStatus("fellow");
            sub.setNextPaymentDate(null);
            memberSubscriptionRepository.save(sub);
        });

        Map<String, Object> result = new HashMap<>();
        result.put("tier", MembershipTier.FELLOW.name());
        result.put("message", "Member promoted to Fellow. They are now a lifetime member with no further dues.");

        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Member promoted to Fellow.")
                        .data(result)
                        .build());
    }
    @PostMapping("/members")
    public ResponseEntity<ApiResponse<Map<String, String>>> createManualMember(
            @Valid @RequestBody CreateMemberRequest request,
            Authentication authentication) {
        UUID adminId = getAdminId(authentication);

        // Check email not already used (case-insensitive)
        String email = com.gnsw.gnsw_backend.util.EmailUtil.normalize(request.getEmail());
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("A user with this email already exists.");
        }

        MembershipTier tier = MembershipTier.valueOf(request.getTier().toUpperCase());

        // Serialize professional-ID allocation across concurrent requests.
        userRepository.lockProfessionalIdSequence();

        // Generate professional ID
        String professionalId = generateProfessionalId(tier);

        // Create User (ACCEPTED immediately)
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(email)
                .addressLine1(request.getAddressLine1() != null ? request.getAddressLine1() : "")
                .addressLine2(request.getAddressLine2())
                .city(request.getCity() != null ? request.getCity() : "")
                .stateProvince(request.getStateProvince() != null ? request.getStateProvince() : "")
                .zipPostalCode(request.getZipPostalCode() != null ? request.getZipPostalCode() : "")
                .country(request.getCountry() != null ? request.getCountry() : "Nigeria")
                .tier(tier)
                .status(UserStatus.ACCEPTED)
                .role("ROLE_MEMBER")
                .professionalId(professionalId)
                .approvedAt(LocalDateTime.now())
                .approvedBy(adminId)
                // NOT setting emailVerifiedAt — manual members start unverified
                // until they use the password-set link (proving email ownership)
                .build();

        // Generate password-set token
        String token = UUID.randomUUID().toString();
        user.setPasswordSetToken(token);
        user.setPasswordSetTokenExpiresAt(LocalDateTime.now().plusHours(48));

        user = userRepository.save(user);

        // Create Member record
        Member member = Member.builder()
                .userId(user.getId())
                .organisation(request.getOrganisation())
                .phone(request.getPhone())
                .build();
        memberRepository.save(member);

        // Manual members follow the same flow as accepted applicants: they are
        // held on the pay-wall until their first annual dues are paid.
        try {
            newApplicationService.ensurePaymentDueSubscription(user.getId(), tier.name());
        } catch (Exception e) {
            System.err.println("Failed to create payment_due marker: " + e.getMessage());
        }

        // Send welcome email with password-set link
        try {
            emailService.sendApprovalEmail(user.getEmail(), user.getFirstName(), tier.name(),
                    professionalId, token, null,
                    com.gnsw.gnsw_backend.util.MembershipFees.annualFeeLabel(tier.name()));
        } catch (Exception e) {
            // Log but don't fail member creation
            System.err.println("Failed to send welcome email: " + e.getMessage());
        }

        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(true)
                        .message("Member created successfully. Welcome email sent.")
                        .data(Map.of("professionalId", professionalId))
                        .build());
    }

    @GetMapping("/payments/orphaned")
    public ResponseEntity<ApiResponse<java.util.List<com.gnsw.gnsw_backend.entity.Payment>>> getOrphanedPayments() {
        java.util.List<com.gnsw.gnsw_backend.entity.Payment> orphaned = paymentRepository.findOrphanedPayments();
        return ResponseEntity.ok()
                .body(ApiResponse.<java.util.List<com.gnsw.gnsw_backend.entity.Payment>>builder()
                        .success(true)
                        .message("Orphaned payments retrieved.")
                        .data(orphaned)
                        .build());
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats() {
        java.time.LocalDateTime monthStart = java.time.LocalDate.now().withDayOfMonth(1).atStartOfDay();

        // Tier distribution: [tier_name, count][] -> map.
        Map<String, Long> membersByTier = new java.util.LinkedHashMap<>();
        for (Object[] row : userRepository.countAcceptedByTier()) {
            String tier = row[0] != null ? ((MembershipTier) row[0]).name() : "UNKNOWN";
            membersByTier.put(tier, ((Number) row[1]).longValue());
        }

        // Revenue by month: [YYYY-MM, sum][] -> map.
        Map<String, Long> revenueByMonth = new java.util.LinkedHashMap<>();
        for (Object[] row : paymentRepository.getRevenueByMonth()) {
            revenueByMonth.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("pending", newApplicationService.getPendingCount());
        stats.put("approved", newApplicationService.getApprovedCount());
        stats.put("rejected", newApplicationService.getRejectedCount());
        stats.put("totalRevenue", paymentRepository.getTotalRevenue());
        stats.put("successfulPayments", paymentRepository.countByStatus(com.gnsw.gnsw_backend.enums.PaymentStatus.SUCCESS));
        stats.put("totalMembers", userRepository.countByStatus(UserStatus.ACCEPTED));
        stats.put("newThisMonth", userRepository.countAcceptedSince(monthStart));
        stats.put("activeSubscriptions", memberSubscriptionRepository.countByStatusIgnoreCase("active"));
        stats.put("membersByTier", membersByTier);
        stats.put("revenueByMonth", revenueByMonth);

        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Stats retrieved.")
                        .data(stats)
                        .build());
    }

    /**
     * Builds a PageRequest with an optional safe sort. No sort is supplied (or
     * the field is unrecognised) → defaults to {@code createdAt} descending so
     * the newest records sit at the top, matching the admin tables' default.
     */
    private PageRequest buildPageRequest(int page, int size, String sort, String order) {
        Sort.Direction dir = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String field = "createdAt";
        if (sort != null && !sort.isBlank()) {
            field = switch (sort.toLowerCase()) {
                case "firstname", "name" -> "firstName";
                case "lastname" -> "lastName";
                case "email" -> "email";
                case "date", "created", "createdat" -> "createdAt";
                case "approved", "approvedat", "joined" -> "approvedAt";
                default -> "createdAt";
            };
        }
        return PageRequest.of(page, size, Sort.by(Sort.Order.by(field).with(dir)));
    }

    /**
     * Extract the admin's user ID from the authentication context.
     * Looks up the User by username (set during login) and returns their ID.
     */
    private UUID getAdminId(Authentication authentication) {
        // Admin tokens resolve against the separate admin_users identity.
        Object principal = authentication.getPrincipal();
        if (principal instanceof AdminUser adminUser) {
            return adminUser.getId();
        }
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .map(User::getId)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found."));
    }

    private String generateProfessionalId(MembershipTier tier) {
        String yearPrefix = "GNSW-" + Year.now().getValue() + "-";
        String maxId = userRepository.findMaxProfessionalIdByYearPrefix(yearPrefix);
        int nextNumber = 1;
        if (maxId != null && !maxId.isEmpty()) {
            String[] parts = maxId.split("-");
            if (parts.length == 3) {
                nextNumber = Integer.parseInt(parts[2]) + 1;
            }
        }
        return yearPrefix + String.format("%03d", nextNumber);
    }

    @Data
    public static class CreateMemberRequest {
        @NotBlank private String firstName;
        @NotBlank private String lastName;
        @NotBlank @Email private String email;
        @NotBlank @Pattern(regexp = "AFFILIATE|ASSOCIATE|MEMBER", message = "Tier must be AFFILIATE, ASSOCIATE, or MEMBER")
        private String tier;
        private String organisation;
        private String phone;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String stateProvince;
        private String zipPostalCode;
        private String country;
    }

    @Data
    public static class ApproveRequest {
        private String customMessage;
    }

    @Data
    public static class RejectRequest {
        @NotBlank(message = "Rejection reason is required")
        private String reason;
    }

    /**
     * Returns a human-readable subscription state for the admin members list.
     * A member whose subscription is not in good standing (or has none) shows
     * as "Inactive"; everyone else shows their subscription state.
     */
    private String subscriptionStatusLabel(UUID userId) {
        return memberSubscriptionRepository.findByUserId(userId)
                .map(sub -> {
                    String status = sub.getStatus() == null ? "" : sub.getStatus().toLowerCase();
                    switch (status) {
                        case "active", "pending": return "Active";
                        case "payment_due": return "Payment Due";
                        case "past_due": return "Past due";
                        case "cancelled": return "Cancelled";
                        case "expired": return "Inactive";
                        default: return status.isEmpty() ? "Inactive" : status.substring(0, 1).toUpperCase() + status.substring(1);
                    }
                })
                .orElse("Inactive");
    }
}