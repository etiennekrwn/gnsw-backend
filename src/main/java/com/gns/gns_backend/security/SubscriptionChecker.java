package com.gns.gns_backend.security;

import com.gns.gns_backend.entity.MemberSubscription;
import com.gns.gns_backend.entity.User;
import com.gns.gns_backend.repository.MemberSubscriptionRepository;
import com.gns.gns_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

@Component("subscriptionChecker")
@RequiredArgsConstructor
public class SubscriptionChecker {

    private final MemberSubscriptionRepository memberSubscriptionRepository;
    private final UserRepository userRepository;

    @Value("${app.subscription.grace-days:5}")
    private int graceDays;

    public boolean hasActive(Authentication auth) {
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }

        String username = auth.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) {
            return false;
        }

        Optional<MemberSubscription> sub = memberSubscriptionRepository.findByUserId(user.getId());
        if (sub.isEmpty()) {
            return false;
        }
        MemberSubscription s = sub.get();
        String status = s.getStatus() == null ? "" : s.getStatus().toLowerCase();

        if ("active".equals(status) || "pending".equals(status)) {
            return true;
        }
        if ("past_due".equals(status)) {
            // Member stays entitled through the grace window after the first failure.
            return s.getGraceStartedAt() == null
                    || s.getGraceStartedAt().plusDays(graceDays).isAfter(LocalDateTime.now());
        }
        if ("cancelled".equals(status)) {
            // Access lasts until the end of the paid year.
            return s.getNextPaymentDate() != null && s.getNextPaymentDate().isAfter(LocalDateTime.now());
        }
        return false;
    }
}
