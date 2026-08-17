package com.gnsw.gnsw_backend.repository;

import com.gnsw.gnsw_backend.entity.User;
import com.gnsw.gnsw_backend.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByProfessionalId(String professionalId);

    Optional<User> findByPasswordSetToken(String passwordSetToken);

    Optional<User> findByPasswordResetToken(String passwordResetToken);

    List<User> findByStatusAndPasswordSetAtIsNullAndPasswordSetTokenExpiresAtBefore(
            UserStatus status, LocalDateTime now);

    Page<User> findByStatus(UserStatus status, Pageable pageable);

    long countByStatus(UserStatus status);

    @Query("SELECT COALESCE(MAX(u.professionalId), 'GNSW-0000-000') FROM User u WHERE u.professionalId LIKE ?1%")
    String findMaxProfessionalIdByYearPrefix(String yearPrefix);

    /**
     * Serializes professional-ID allocation across concurrent requests using a
     * Postgres advisory (transaction-scoped) lock. The lock is held until the
     * surrounding transaction commits or rolls back.
     */
    @Query(value = "SELECT pg_advisory_xact_lock(hashtext('gnsw_professional_id_seq'))", nativeQuery = true)
    void lockProfessionalIdSequence();
}