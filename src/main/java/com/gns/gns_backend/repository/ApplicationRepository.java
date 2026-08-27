package com.gns.gns_backend.repository;

import com.gns.gns_backend.entity.Application;
import com.gns.gns_backend.enums.ApplicationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {

    Optional<Application> findByEmail(String email);

    Optional<Application> findByPaymentReference(String paymentReference);

    Page<Application> findByStatus(ApplicationStatus status, Pageable pageable);

    long countByStatus(ApplicationStatus status);

    /**
     * Fetches the application with a pessimistic write lock so concurrent
     * approve/reject calls on the same application are serialized.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Application a where a.id = :id")
    Optional<Application> findByIdForUpdate(@Param("id") UUID id);
}