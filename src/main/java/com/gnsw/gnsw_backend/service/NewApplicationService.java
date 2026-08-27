package com.gnsw.gnsw_backend.service;

import com.gnsw.gnsw_backend.entity.Application;
import com.gnsw.gnsw_backend.entity.Member;
import com.gnsw.gnsw_backend.entity.MemberSubscription;
import com.gnsw.gnsw_backend.entity.Payment;
import com.gnsw.gnsw_backend.entity.User;
import com.gnsw.gnsw_backend.enums.ApplicationStatus;
import com.gnsw.gnsw_backend.enums.MembershipTier;
import com.gnsw.gnsw_backend.enums.PaymentStatus;
import com.gnsw.gnsw_backend.enums.UserStatus;
import com.gnsw.gnsw_backend.event.ApplicationApprovedEvent;
import com.gnsw.gnsw_backend.repository.ApplicationRepository;
import com.gnsw.gnsw_backend.repository.MemberRepository;
import com.gnsw.gnsw_backend.repository.MemberSubscriptionRepository;
import com.gnsw.gnsw_backend.repository.PaymentRepository;
import com.gnsw.gnsw_backend.repository.UserRepository;
import com.gnsw.gnsw_backend.util.EmailUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final PaymentRepository paymentRepository;
    private final MemberSubscriptionRepository memberSubscriptionRepository;
    private final EmailService emailService;
    private final PaymentService paymentService;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Value("${paystack.plans.affiliate}")
    private String affiliatePlanCode;

    @Value("${paystack.plans.associate}")
    private String associatePlanCode;

    @Value("${paystack.plans.member}")
    private String memberPlanCode;

    /**
     * Check if an email is available for a new application.
     * Throws IllegalArgumentException if the email already has an application or user account.
     */
    public void checkEmailAvailable(String email) {
        Optional<Application> existing = applicationRepository.findByEmail(EmailUtil.normalize(email));
        if (existing.isPresent() && existing.get().getStatus() != ApplicationStatus.REJECTED) {
            throw new IllegalArgumentException("An application with this email already exists.");
        }
        if (userRepository.findByEmail(EmailUtil.normalize(email)).isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }
    }

    /**
     * Create an application after successful payment.
     * This is the ONLY place an application record is created.
     */
    @Transactional
    public Application createApplication(String firstName, String lastName, String email,
                                          String addressLine1, String addressLine2,
                                          String city, String stateProvince,
                                          String zipPostalCode, String country,
                                          String phone,
                                          String linkedInProfile, String socials,
                                          String bio, String reasonForJoining,
                                          String sectors, String speechTypes, String languages,
                                          String membershipTier,
                                          String paymentReference, int paymentAmount) {
        // Check if email already has an application (REJECTED ones may be reused).
        String normalizedEmail = EmailUtil.normalize(email);
        Optional<Application> existing = applicationRepository.findByEmail(normalizedEmail);
        boolean reapply = existing.isPresent() && existing.get().getStatus() == ApplicationStatus.REJECTED;
        if (existing.isPresent() && !reapply) {
            throw new IllegalArgumentException("An application with this email already exists.");
        }

        // Verify the payment with Paystack before creating the application.
        // Skip verification for demo/test references (e.g. DEMO-REF-*) so the
        // application + payment record can be created without a real Paystack charge.
        String authorizationCode = null;
        if (!paymentReference.startsWith("DEMO-")) {
            try {
                authorizationCode = paymentService.verifyPaymentByReference(paymentReference, paymentAmount);
            } catch (Exception e) {
                throw new RuntimeException("Payment verification failed: " + e.getMessage());
            }
        }

        MembershipTier tier = MembershipTier.valueOf(membershipTier);

        Application application = reapply ? existing.get() : new Application();
        application.setFirstName(firstName);
        application.setLastName(lastName);
        application.setEmail(normalizedEmail);
        application.setAddressLine1(addressLine1);
        application.setAddressLine2(addressLine2);
        application.setCity(city);
        application.setStateProvince(stateProvince);
        application.setZipPostalCode(zipPostalCode);
        application.setCountry(country);
        application.setPhone(phone);
        application.setLinkedInProfile(linkedInProfile);
        application.setSocials(socials);
        application.setBio(bio);
        application.setReasonForJoining(reasonForJoining);
        application.setSectors(sectors);
        application.setSpeechTypes(speechTypes);
        application.setLanguages(languages);
        application.setMembershipTier(tier);
        application.setStatus(ApplicationStatus.PENDING);
        application.setPaymentReference(paymentReference);
        application.setPaymentAmount(paymentAmount);
        application.setPaymentStatus("SUCCESS");
        application.setEmailVerified(true);
        application.setAuthorizationCode(authorizationCode);
        // Reset review state - this may be a re-application (REJECTED -> PENDING).
        application.setReviewedAt(null);
        application.setReviewedBy(null);
        application.setRejectionReason(null);
        application.setSubscriptionCode(null);
        application.setPlanCode(null);
        application.setEmailToken(null);

        application = applicationRepository.save(application);

        // Record the payment in its OWN transaction (REQUIRES_NEW) so that a payment
        // failure can NEVER roll back the application creation, and vice versa.
        // This protects against money-loss: the user's payment is always captured.
        recordPayment(paymentReference, paymentAmount, tier);

        // Notify the applicant that their application was submitted successfully.
        // Paystack separately handles the payment-confirmation email, so this is a
        // dedicated "application received" confirmation. Runs async so it never
        // blocks or fails the API response.
        try {
            emailService.sendApplicationReceivedEmail(
                    application.getEmail(),
                    application.getFirstName(),
                    tier.name(),
                    application.getId().toString());
        } catch (Exception e) {
            log.error("Failed to send application-received email to {}: {}",
                    application.getEmail(), e.getMessage());
        }

        return application;
    }

    /**
     * Record a successful payment in its own independent transaction.
     * Runs separately from application creation so a failure in either
     * does not lose the other. Skips duplicates by reference.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPayment(String paymentReference, int paymentAmount, MembershipTier tier) {
        try {
            if (paymentRepository.findByReference(paymentReference).isEmpty()) {
                Payment payment = Payment.builder()
                        .reference(paymentReference)
                        .amount(paymentAmount)
                        .tier(tier)
                        .status(PaymentStatus.SUCCESS)
                        .paidAt(LocalDateTime.now())
                        .build();
                paymentRepository.save(payment);
                log.info("Payment recorded: {} ({})", paymentAmount, paymentReference);
            } else {
                log.warn("Payment with reference {} already exists; skipping duplicate record.", paymentReference);
            }
        } catch (Exception e) {
            log.error("Failed to record payment {}: {}", paymentReference, e.getMessage());
        }
    }

    /**
     * Approve an application - creates User + Member records.
     */
    @Transactional
    public String approveApplication(UUID applicationId, UUID adminId, String customMessage) {
        Application application = applicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));

        if (application.getStatus() == ApplicationStatus.APPROVED) {
            throw new IllegalArgumentException("This application has already been approved.");
        }
        if (application.getStatus() == ApplicationStatus.REJECTED) {
            throw new IllegalArgumentException("This application has been rejected and cannot be approved.");
        }

        // Guard against approving an application whose email already belongs to a
        // user (e.g. a manually-created member). Fail fast instead of a 500.
        if (userRepository.findByEmail(application.getEmail()).isPresent()) {
            throw new IllegalArgumentException("A user account with this email already exists. Application cannot be approved.");
        }

        // Serialize professional-ID allocation across concurrent approvals.
        userRepository.lockProfessionalIdSequence();
        // Generate professional ID
        String professionalId = generateProfessionalId(application.getMembershipTier());

        // Create User
        User user = User.builder()
                .firstName(application.getFirstName())
                .lastName(application.getLastName())
                .email(application.getEmail())
                .addressLine1(application.getAddressLine1())
                .addressLine2(application.getAddressLine2())
                .city(application.getCity())
                .stateProvince(application.getStateProvince())
                .zipPostalCode(application.getZipPostalCode())
                .country(application.getCountry())
                .tier(application.getMembershipTier())
                .status(UserStatus.ACCEPTED)
                .role("ROLE_MEMBER")
                .professionalId(professionalId)
                .approvedAt(LocalDateTime.now())
                .approvedBy(adminId)
                .emailVerifiedAt(LocalDateTime.now())
                .build();

        // Generate password-set token
        String token = UUID.randomUUID().toString();
        user.setPasswordSetToken(token);
        user.setPasswordSetTokenExpiresAt(LocalDateTime.now().plusHours(48));

        user = userRepository.save(user);

        // Create Member record — copy professional fields from the application
        Member member = Member.builder()
                .userId(user.getId())
                .phone(application.getPhone())
                .bio(application.getBio())
                .sectors(application.getSectors())
                .speechTypes(application.getSpeechTypes())
                .languages(application.getLanguages())
                .linkedInProfile(application.getLinkedInProfile())
                .socials(application.getSocials())
                .reasonForJoining(application.getReasonForJoining())
                .build();
        memberRepository.save(member);

        // Deferred subscription billing: the applicant paid a one-off fee at checkout.
        // The Paystack subscription is created NOW so the first renewal lands one year
        // after approval (the annual cycle starts counting from approval, unlimited
        // renewals). Legacy in-flight subscriptions created by the old plan-at-checkout
        // flow are cancelled first so their cycle is re-anchored to approval.
        LocalDateTime startDate = LocalDateTime.now().plusYears(1);
        String subscriptionCode = null;
        String planCode = null;
        String emailToken = application.getEmailToken();
        String authorizationCode = application.getAuthorizationCode();

        if (authorizationCode != null && !authorizationCode.isBlank()) {
            String legacyCode = application.getSubscriptionCode();
            if (legacyCode != null && !legacyCode.isBlank()) {
                try {
                    paymentService.cancelSubscription(legacyCode);
                } catch (Exception e) {
                    log.error("Failed to cancel legacy subscription {}: {}", legacyCode, e.getMessage());
                }
            }
            try {
                var sub = paymentService.createSubscription(
                        application.getEmail(),
                        application.getMembershipTier().name(),
                        authorizationCode,
                        startDate);
                subscriptionCode = sub.subscriptionCode();
                planCode = sub.planCode();
                startDate = sub.nextPaymentDate();
            } catch (Exception e) {
                // Never fail approval because of a subscription issue; log it so it
                // can be reconciled later.
                log.error("Failed to create Paystack subscription for {}: {}",
                        application.getEmail(), e.getMessage());
            }
        }

        createMemberSubscription(user, application, subscriptionCode, planCode, emailToken, startDate);

        // Update application status
        application.setStatus(ApplicationStatus.APPROVED);
        application.setReviewedAt(LocalDateTime.now());
        application.setReviewedBy(adminId);
        application.setSubscriptionCode(subscriptionCode);
        application.setPlanCode(planCode);
        applicationRepository.save(application);

        // Publish an AFTER_COMMIT event so the welcome email is only sent once
        // this transaction has successfully committed (never before, never on rollback).
        publishApprovalEvent(application, user, professionalId, token, customMessage);

        return professionalId;
    }

    /**
     * Publishes an AFTER_COMMIT approval event. The welcome email is sent by
     * ApplicationApprovalListener only after the approval transaction commits.
     */
    private void publishApprovalEvent(Application application, User user, String professionalId, String token, String customMessage) {
        applicationEventPublisher.publishEvent(new ApplicationApprovedEvent(
                user.getEmail(), user.getFirstName(),
                application.getMembershipTier().name(),
                professionalId, token, customMessage));
    }

    /**
     * Reject an application.
     */
    @Transactional
    public void rejectApplication(UUID applicationId, UUID adminId, String reason) {
        Application application = applicationRepository.findByIdForUpdate(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));

        if (application.getStatus() == ApplicationStatus.REJECTED) {
            throw new IllegalArgumentException("This application has already been rejected.");
        }
        if (application.getStatus() == ApplicationStatus.APPROVED) {
            throw new IllegalArgumentException("This application has already been approved and cannot be rejected.");
        }

        application.setStatus(ApplicationStatus.REJECTED);
        application.setReviewedAt(LocalDateTime.now());
        application.setReviewedBy(adminId);
        application.setRejectionReason(reason);

        // A legacy (plan-at-checkout) subscription must be cancelled so a rejected
        // applicant is never charged next year. New-flow applicants have no
        // subscription yet, so there is nothing to cancel.
        String legacySubscriptionCode = application.getSubscriptionCode();
        if (legacySubscriptionCode != null && !legacySubscriptionCode.isBlank()) {
            try {
                paymentService.cancelSubscription(legacySubscriptionCode);
                log.info("Cancelled subscription {} for rejected application {}",
                        legacySubscriptionCode, application.getEmail());
            } catch (Exception e) {
                log.error("Failed to cancel subscription {} on rejection: {}",
                        legacySubscriptionCode, e.getMessage());
            }
        }

        applicationRepository.save(application);

        // Send rejection email outside transaction to prevent rollback on email failure
        try {
            emailService.sendRejectionEmail(application.getEmail(), application.getFirstName(), reason);
        } catch (Exception e) {
            log.error("Failed to send rejection email to {}: {}", application.getEmail(), e.getMessage());
        }
    }

    public Page<Application> getApplications(ApplicationStatus status, Pageable pageable) {
        return applicationRepository.findByStatus(status, pageable);
    }

    public Page<Application> getAllApplications(Pageable pageable) {
        return applicationRepository.findAll(pageable);
    }

    public Application getApplication(UUID id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));
    }

    public long getPendingCount() {
        return applicationRepository.countByStatus(ApplicationStatus.PENDING);
    }

    public long getApprovedCount() {
        return applicationRepository.countByStatus(ApplicationStatus.APPROVED);
    }

    public long getRejectedCount() {
        return applicationRepository.countByStatus(ApplicationStatus.REJECTED);
    }

    /**
     * Create a member_subscriptions row when an application is approved.
     * Uses the subscription captured by the Paystack webhook (subscription.create)
     * when available; otherwise synthesizes it from the configured plan code.
     */
    private void createMemberSubscription(User user, Application application,
                                          String subscriptionCode, String planCode,
                                          String emailToken, LocalDateTime nextPaymentDate) {
        try {
            boolean exists = memberSubscriptionRepository.findByUserId(user.getId()).isPresent();
            if (exists) {
                log.info("Subscription already exists for user: {}; skipping.", user.getEmail());
                return;
            }

            if (planCode == null || planCode.isBlank() || "UNKNOWN".equals(planCode)) {
                planCode = getPlanCode(application.getMembershipTier().name());
            }

            MemberSubscription sub = MemberSubscription.builder()
                    .userId(user.getId())
                    .subscriptionCode(subscriptionCode)
                    .planCode(planCode)
                    .emailToken(emailToken)
                    .status("active")
                    .nextPaymentDate(nextPaymentDate)
                    .build();
            memberSubscriptionRepository.save(sub);
            log.info("Member subscription created for user: {}", user.getEmail());
        } catch (Exception e) {
            // Never fail approval because of a subscription issue;
            // log it so it can be reconciled later.
            log.error("Failed to create member subscription for user {}: {}", user.getEmail(), e.getMessage());
        }
    }

    private String getPlanCode(String tierName) {
        return switch (tierName) {
            case "AFFILIATE" -> affiliatePlanCode;
            case "ASSOCIATE" -> associatePlanCode;
            case "MEMBER" -> memberPlanCode;
            default -> null;
        };
    }

    private String generateProfessionalId(MembershipTier tier) {
        String yearPrefix = "GNS-" + Year.now().getValue() + "-";
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
}