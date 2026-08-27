package com.gns.gns_backend.repository;

import com.gns.gns_backend.entity.User;
import com.gns.gns_backend.enums.UserStatus;
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

    /** Active ACCEPTED members grouped by tier, for analytics tier-distribution. */
    @Query("SELECT u.tier AS tier, COUNT(u) AS cnt FROM User u WHERE u.status = 'ACCEPTED' GROUP BY u.tier")
    List<Object[]> countAcceptedByTier();

    /** Members who became ACCEPTED since a cutoff (for "new this month"). */
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = 'ACCEPTED' AND u.approvedAt >= :since")
    long countAcceptedSince(java.time.LocalDateTime since);

    /** Members approved grouped by year/month, for the signup chart. */
    @Query("SELECT FUNCTION('to_char', u.approvedAt, 'YYYY-MM') AS ym, COUNT(u) AS cnt " +
           "FROM User u WHERE u.approvedAt IS NOT NULL " +
           "GROUP BY FUNCTION('to_char', u.approvedAt, 'YYYY-MM') ORDER BY ym")
    List<Object[]> countAcceptedByMonth();

    @Query("SELECT COALESCE(MAX(u.professionalId), 'GNS-0000-000') FROM User u WHERE u.professionalId LIKE ?1%")
    String findMaxProfessionalIdByYearPrefix(String yearPrefix);

    /**
     * Serializes professional-ID allocation across concurrent requests using a
     * Postgres advisory (transaction-scoped) lock. The lock is held until the
     * surrounding transaction commits or rolls back.
     */
    @Query(value = "SELECT pg_advisory_xact_lock(hashtext('gns_professional_id_seq'))", nativeQuery = true)
    void lockProfessionalIdSequence();
}
