package com.gnsw.gnsw_backend.service;

import com.gnsw.gnsw_backend.dto.request.ApplicationRequest;
import com.gnsw.gnsw_backend.entity.User;
import com.gnsw.gnsw_backend.enums.MembershipTier;
import com.gnsw.gnsw_backend.enums.UserStatus;
import com.gnsw.gnsw_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final UserRepository userRepository;
    private final OtpService otpService;

    public Map<String, Object> createApplication(ApplicationRequest request) {
        // Check if email already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        // Validate tier is not FELLOW
        MembershipTier tier = MembershipTier.valueOf(request.getMembershipTier());
        if (tier == MembershipTier.FELLOW) {
            throw new IllegalArgumentException("FELLOW tier cannot be applied for via this form.");
        }

        // Create user with PENDING status
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .stateProvince(request.getStateProvince())
                .zipPostalCode(request.getZipPostalCode())
                .country(request.getCountry())
                .tier(tier)
                .status(UserStatus.PENDING)
                .role("ROLE_MEMBER")
                .build();

        user = userRepository.save(user);

        // Generate and send OTP
        otpService.generateAndSendOtp(user.getEmail());

        return Map.of(
                "applicationId", user.getId(),
                "email", user.getEmail(),
                "tier", user.getTier().name()
        );
    }

    public User getApplication(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));
    }

    public void markEmailVerified(UUID id) {
        User user = getApplication(id);
        user.setEmailVerifiedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
    }
}
