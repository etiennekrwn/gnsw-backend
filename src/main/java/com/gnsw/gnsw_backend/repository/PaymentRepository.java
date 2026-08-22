package com.gnsw.gnsw_backend.repository;

import com.gnsw.gnsw_backend.entity.Payment;
import com.gnsw.gnsw_backend.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByReference(String reference);

    Optional<Payment> findByRefundReference(String refundReference);

    Optional<Payment> findByUserIdAndStatus(UUID userId, PaymentStatus status);

    boolean existsByUserIdAndStatus(UUID userId, PaymentStatus status);

    long countByStatus(PaymentStatus status);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.status = 'SUCCESS'")
    long getTotalRevenue();

    /** Successful payment total grouped by month (YYYY-MM), for the revenue chart. */
    @Query("SELECT FUNCTION('to_char', p.paidAt, 'YYYY-MM') AS ym, COALESCE(SUM(p.amount), 0) AS total " +
           "FROM Payment p WHERE p.status = 'SUCCESS' AND p.paidAt IS NOT NULL " +
           "GROUP BY FUNCTION('to_char', p.paidAt, 'YYYY-MM') ORDER BY ym")
    List<Object[]> getRevenueByMonth();

    // Find successful payments that have no matching application (orphaned payments)
    @Query("SELECT p FROM Payment p WHERE p.status = 'SUCCESS' AND p.reference NOT IN (SELECT a.paymentReference FROM Application a WHERE a.paymentReference IS NOT NULL)")
    java.util.List<Payment> findOrphanedPayments();
}
