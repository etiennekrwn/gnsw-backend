package com.gnsw.gnsw_backend.repository;

import com.gnsw.gnsw_backend.entity.MemberSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberSubscriptionRepository extends JpaRepository<MemberSubscription, UUID> {
    Optional<MemberSubscription> findByUserId(UUID userId);
    Optional<MemberSubscription> findBySubscriptionCode(String subscriptionCode);
}
