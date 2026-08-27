package com.gns.gns_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "member_subscriptions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MemberSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "subscription_code", unique = true)
    private String subscriptionCode;

    @Column(name = "plan_code", nullable = false)
    private String planCode;

    @Column(name = "email_token")
    private String emailToken;

    @Column(nullable = false)
    private String status; // active, past_due, cancelled

    @Column(name = "next_payment_date")
    private LocalDateTime nextPaymentDate;

    @Column(name = "grace_started_at")
    private LocalDateTime graceStartedAt;

    @Column(name = "last_payment_attempt_at")
    private LocalDateTime lastPaymentAttemptAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = "pending";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
