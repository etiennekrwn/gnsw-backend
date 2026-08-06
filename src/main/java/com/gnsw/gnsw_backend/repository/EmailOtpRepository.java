package com.gnsw.gnsw_backend.repository;

import com.gnsw.gnsw_backend.entity.EmailOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface EmailOtpRepository extends JpaRepository<EmailOtp, UUID> {

    Optional<EmailOtp> findTopByEmailAndVerifiedAtIsNullOrderByCreatedAtDesc(String email);
}