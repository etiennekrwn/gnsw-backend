package com.gnsw.gnsw_backend.service;

import com.gnsw.gnsw_backend.entity.EmailOtp;
import com.gnsw.gnsw_backend.repository.EmailOtpRepository;
import com.gnsw.gnsw_backend.util.EmailUtil;
import com.gnsw.gnsw_backend.util.OtpGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final EmailOtpRepository emailOtpRepository;
    private final EmailService emailService;

    private static final int OTP_EXPIRY_MINUTES = 10;
    private static final int MAX_ATTEMPTS = 5;

    public void generateAndSendOtp(String email) {
        String normalized = EmailUtil.normalize(email);
        String code = OtpGenerator.generateOtp();

        EmailOtp otp = EmailOtp.builder()
                .email(normalized)
                .otpCode(code)
                .expiresAt(LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES))
                .attempts(0)
                .build();

        emailOtpRepository.save(otp);
        emailService.sendOtpEmail(normalized, code);
    }

    public void verifyOtp(String email, String code) {
        String normalized = EmailUtil.normalize(email);
        EmailOtp otp = emailOtpRepository
                .findTopByEmailAndVerifiedAtIsNullOrderByCreatedAtDesc(normalized)
                .orElseThrow(() -> new IllegalArgumentException("No OTP found for this email. Please request a new one."));

        if (otp.getVerifiedAt() != null) {
            throw new IllegalArgumentException("This OTP has already been verified.");
        }

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("OTP has expired. Please request a new one.");
        }

        if (otp.getAttempts() >= MAX_ATTEMPTS) {
            throw new IllegalArgumentException("Too many failed attempts. Please request a new OTP.");
        }

        if (!otp.getOtpCode().equals(code)) {
            otp.setAttempts(otp.getAttempts() + 1);
            emailOtpRepository.save(otp);
            throw new IllegalArgumentException("Invalid OTP code. " + (MAX_ATTEMPTS - otp.getAttempts()) + " attempts remaining.");
        }

        otp.setVerifiedAt(LocalDateTime.now());
        emailOtpRepository.save(otp);
    }
}