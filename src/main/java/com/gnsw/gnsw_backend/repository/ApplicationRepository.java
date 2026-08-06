package com.gnsw.gnsw_backend.repository;

import com.gnsw.gnsw_backend.entity.Application;
import com.gnsw.gnsw_backend.enums.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    Optional<Application> findByEmail(String email);

    Optional<Application> findByPaymentReference(String paymentReference);

    Page<Application> findByStatus(ApplicationStatus status, Pageable pageable);

    long countByStatus(ApplicationStatus status);
}