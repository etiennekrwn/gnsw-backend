package com.gnsw.gnsw_backend.security;

import com.gnsw.gnsw_backend.entity.MemberSubscription;
import com.gnsw.gnsw_backend.entity.User;
import com.gnsw.gnsw_backend.repository.MemberSubscriptionRepository;
import com.gnsw.gnsw_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("subscriptionChecker")
@RequiredArgsConstructor
public class SubscriptionChecker {

    private final MemberSubscriptionRepository memberSubscriptionRepository;
    private final UserRepository userRepository;

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
        return sub.isPresent() && "active".equals(sub.get().getStatus());
    }
}
