package com.gnsw.gnsw_backend.service;

import com.gnsw.gnsw_backend.entity.Member;
import com.gnsw.gnsw_backend.entity.Payment;
import com.gnsw.gnsw_backend.entity.User;
import com.gnsw.gnsw_backend.enums.MembershipTier;
import com.gnsw.gnsw_backend.enums.PaymentStatus;
import com.gnsw.gnsw_backend.enums.UserStatus;
import com.gnsw.gnsw_backend.repository.MemberRepository;
import com.gnsw.gnsw_backend.repository.PaymentRepository;
import com.gnsw.gnsw_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminApplicationService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final MemberRepository memberRepository;
    private final EmailService emailService;

    public Page<User> getApplications(UserStatus status, Pageable pageable) {
        return userRepository.findByStatus(status, pageable);
    }

    public User getApplicationDetail(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));
    }

    public String approveApplication(UUID applicationId, UUID adminId, String customMessage) {
        User user = userRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));

        if (user.getStatus() != UserStatus.PENDING) {
            throw new IllegalArgumentException("Application is not in PENDING status.");
        }

        // Check payment is confirmed
        Optional<Payment> payment = paymentRepository.findByUserIdAndStatus(user.getId(), PaymentStatus.SUCCESS);
        if (payment.isEmpty()) {
            throw new IllegalArgumentException("Payment has not been confirmed for this application.");
        }

        // Check email verified
        if (user.getEmailVerifiedAt() == null) {
            throw new IllegalArgumentException("Email has not been verified for this application.");
        }

        // Generate professional ID
        String professionalId = generateProfessionalId(user.getTier());

        // Update user status
        user.setStatus(UserStatus.ACCEPTED);
        user.setApprovedAt(LocalDateTime.now());
        user.setApprovedBy(adminId);
        user.setProfessionalId(professionalId);

        // Generate password-set token
        String token = UUID.randomUUID().toString();
        user.setPasswordSetToken(token);
        user.setPasswordSetTokenExpiresAt(LocalDateTime.now().plusHours(48));

        userRepository.save(user);

        // Create member record
        Member member = Member.builder()
                .userId(user.getId())
                .build();
        memberRepository.save(member);

        // Send approval email
        String message = customMessage != null ? customMessage : "Welcome to the Guild! We're excited to have you.";
        emailService.sendApprovalEmail(user.getEmail(), user.getFirstName(), user.getTier().name(), professionalId, token);

        return professionalId;
    }

    public void rejectApplication(UUID applicationId, UUID adminId, String reason) {
        User user = userRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));

        if (user.getStatus() != UserStatus.PENDING) {
            throw new IllegalArgumentException("Application is not in PENDING status.");
        }

        user.setStatus(UserStatus.REJECTED);
        user.setRejectedAt(LocalDateTime.now());
        user.setRejectedBy(adminId);
        user.setRejectionReason(reason);
        userRepository.save(user);

        emailService.sendRejectionEmail(user.getEmail(), user.getFirstName(), reason);
    }

    public String createManualMember(String firstName, String lastName, String email,
                                      String tierStr, boolean sendWelcomeEmail, UUID adminId) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("A user with this email already exists.");
        }

        MembershipTier tier = MembershipTier.valueOf(tierStr.toUpperCase());

        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .tier(tier)
                .status(UserStatus.ACCEPTED)
                .role("ROLE_MEMBER")
                .approvedAt(LocalDateTime.now())
                .approvedBy(adminId)
                .build();

        String professionalId = generateProfessionalId(tier);
        user.setProfessionalId(professionalId);

        if (sendWelcomeEmail) {
            String token = UUID.randomUUID().toString();
            user.setPasswordSetToken(token);
            user.setPasswordSetTokenExpiresAt(LocalDateTime.now().plusHours(48));
        }

        user = userRepository.save(user);

        // Create member record
        Member member = Member.builder()
                .userId(user.getId())
                .build();
        memberRepository.save(member);

        if (sendWelcomeEmail) {
            emailService.sendApprovalEmail(user.getEmail(), user.getFirstName(), tier.name(), professionalId,
                    user.getPasswordSetToken());
        }

        return professionalId;
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

    // Stats for dashboard
    public long getPendingCount() {
        return userRepository.countByStatus(UserStatus.PENDING);
    }

    public long getApprovedCount() {
        return userRepository.countByStatus(UserStatus.ACCEPTED);
    }

    public long getRejectedCount() {
        return userRepository.countByStatus(UserStatus.REJECTED);
    }
}