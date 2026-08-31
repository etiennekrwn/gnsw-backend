package com.gnsw.gnsw_backend.service;

import com.gnsw.gnsw_backend.entity.Application;
import com.gnsw.gnsw_backend.entity.Member;
import com.gnsw.gnsw_backend.entity.MemberSubscription;
import com.gnsw.gnsw_backend.entity.User;
import com.gnsw.gnsw_backend.enums.ApplicationStatus;
import com.gnsw.gnsw_backend.enums.MembershipTier;
import com.gnsw.gnsw_backend.enums.UserStatus;
import com.gnsw.gnsw_backend.event.ApplicationApprovedEvent;
import com.gnsw.gnsw_backend.repository.ApplicationRepository;
import com.gnsw.gnsw_backend.repository.MemberRepository;
import com.gnsw.gnsw_backend.repository.MemberSubscriptionRepository;
import com.gnsw.gnsw_backend.repository.UserRepository;
import com.gnsw.gnsw_backend.util.EmailUtil;
import com.gnsw.gnsw_backend.util.MembershipFees;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
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
     * Create an application (free, no payment). This is the ONLY place an
     * application record is created. Under the new model the applicant is NOT
     * charged at submission; the annual membership fee is collected on the
     * members portal only after the application is accepted.
     */
    @Transactional
    public Application createApplication(String firstName, String lastName, String email,
                                          String addressLine1, String addressLine2,
                                          String city, String stateProvince,
                                          String zipPostalCode, String country,
                                          String phone,
                                          String socialMediaPlatform,
                                          String bio, String reasonForJoining,
                                          String currentProfessionalRole, String favouriteOrator,
                                          String speechwritingTraining, String trainingDetails,
                                          String highestQualification, String currentJobTitle,
                                          String currentOrganization,
                                          String membershipTier) {
        // Check if email already has an application (REJECTED ones may be reused).
        String normalizedEmail = EmailUtil.normalize(email);
        Optional<Application> existing = applicationRepository.findByEmail(normalizedEmail);
        boolean reapply = existing.isPresent() && existing.get().getStatus() == ApplicationStatus.REJECTED;
        if (existing.isPresent() && !reapply) {
            throw new IllegalArgumentException("An application with this email already exists.");
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
        application.setSocialMediaPlatform(socialMediaPlatform);
        application.setBio(bio);
        application.setReasonForJoining(reasonForJoining);
        application.setCurrentProfessionalRole(currentProfessionalRole);
        application.setFavouriteOrator(favouriteOrator);
        application.setSpeechwritingTraining(speechwritingTraining);
        application.setTrainingDetails(trainingDetails);
        application.setHighestQualification(highestQualification);
        application.setCurrentJobTitle(currentJobTitle);
        application.setCurrentOrganization(currentOrganization);
        application.setMembershipTier(tier);
        application.setStatus(ApplicationStatus.PENDING);
        // No payment is collected at submission, so payment fields stay clear.
        application.setPaymentReference(null);
        application.setPaymentAmount(null);
        application.setPaymentStatus(null);
        application.setEmailVerified(true);
        application.setAuthorizationCode(null);
        // Reset review state - this may be a re-application (REJECTED -> PENDING).
        application.setReviewedAt(null);
        application.setReviewedBy(null);
        application.setRejectionReason(null);
        application.setSubscriptionCode(null);
        application.setPlanCode(null);
        application.setEmailToken(null);

        application = applicationRepository.save(application);

        // Notify the applicant that their application was submitted successfully.
        // Under the free-application model there is no payment step here.
        // Runs async so it never blocks or fails the API response.
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
                .currentProfessionalRole(application.getCurrentProfessionalRole())
                .favouriteOrator(application.getFavouriteOrator())
                .speechwritingTraining(application.getSpeechwritingTraining())
                .trainingDetails(application.getTrainingDetails())
                .highestQualification(application.getHighestQualification())
                .currentJobTitle(application.getCurrentJobTitle())
                .currentOrganization(application.getCurrentOrganization())
                .socialMediaPlatform(application.getSocialMediaPlatform())
                .reasonForJoining(application.getReasonForJoining())
                .build();
        memberRepository.save(member);

        // First subscription (annual dues) is NOT created on approval. Under the
        // free-application model we have no stored card authorization yet. Instead we
        // create a "payment_due" subscription marker. The accepted member is directed to
        // the members portal pay-wall, pays their first annual fee, and only THEN is the
        // Paystack subscription created (from the authorization captured on that payment)
        // and the marker flipped to "active". The member stays in "payment_due" (pay-wall
        // always shown, no expiry) until they pay.
        String tierName = application.getMembershipTier().name();
        ensurePaymentDueSubscription(user.getId(), tierName);

        // Update application status
        application.setStatus(ApplicationStatus.APPROVED);
        application.setReviewedAt(LocalDateTime.now());
        application.setReviewedBy(adminId);
        application.setSubscriptionCode(null);
        application.setPlanCode(getPlanCode(tierName));
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
                professionalId, token, customMessage,
                MembershipFees.annualFeeLabel(application.getMembershipTier().name())));
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
     * Creates (or keeps) the "payment_due" subscription marker for a newly
     * accepted member. The member has NOT paid yet — the pay-wall on the members
     * portal stays until their first annual fee is collected. The Paystack
     * subscription (auto-renew) is created later, on that first payment, and the
     * status flips to "active" at that point.
     */
    public void ensurePaymentDueSubscription(UUID userId, String tierName) {
        try {
            Optional<MemberSubscription> existing = memberSubscriptionRepository.findByUserId(userId);
            if (existing.isPresent()) {
                log.info("Subscription already exists for user {}; leaving as-is.", userId);
                return;
            }

            MemberSubscription sub = MemberSubscription.builder()
                    .userId(userId)
                    .subscriptionCode(null)
                    .planCode(getPlanCode(tierName))
                    .status("payment_due")
                    .nextPaymentDate(null)
                    .build();
            memberSubscriptionRepository.save(sub);
            log.info("payment_due subscription marker created for user: {}", userId);
        } catch (Exception e) {
            // Never fail approval because of a subscription issue;
            // log it so it can be reconciled later.
            log.error("Failed to create payment_due marker for user {}: {}", userId, e.getMessage());
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
}