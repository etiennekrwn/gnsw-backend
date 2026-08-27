package com.gns.gns_backend.service;

import com.gns.gns_backend.entity.MemberSubscription;
import com.gns.gns_backend.entity.User;
import com.gns.gns_backend.repository.MemberSubscriptionRepository;
import com.gns.gns_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Owns the subscription state transitions that happen on a schedule rather than
 * in response to a webhook:
 *
 * <ul>
 *   <li>{@code past_due -> expired}: a renewal failed and the grace window elapsed.</li>
 *   <li>{@code cancelled -> expired}: a member cancelled but kept access until the end
 *       of the paid year; once the paid-through date passes, access is lost.</li>
 * </ul>
 *
 * A member with an {@code expired} subscription is shown as "Inactive" in the admin
 * portal, loses posting + full reads, and is hidden from the directory.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionLifecycleService {

    private final MemberSubscriptionRepository memberSubscriptionRepository;
    private final UserRepository userRepository;

    @Value("${app.subscription.grace-days:5}")
    private int graceDays;

    /** Run every night at 02:15 UTC to move paid/due subscriptions that earned no
     *  recovery before the grace window or paid period ended. */
    @Scheduled(cron = "${app.subscription.reconcile-cron:0 15 2 * * *}")
    @Transactional
    public void reconcileExpiredSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        int expiredPastDue = 0;
        int expiredCancelled = 0;

        List<MemberSubscription> pastDue = memberSubscriptionRepository.findByStatusIgnoreCase("past_due");
        for (MemberSubscription sub : pastDue) {
            boolean graceElapsed = sub.getGraceStartedAt() != null
                    && sub.getGraceStartedAt().isBefore(now.minusDays(graceDays));
            if (graceElapsed) {
                markExpired(sub);
                expiredPastDue++;
            }
        }

        List<MemberSubscription> cancelled = memberSubscriptionRepository.findByStatusIgnoreCase("cancelled");
        for (MemberSubscription sub : cancelled) {
            // Access lasts until the end of the paid year. Once the period passes, expire.
            if (sub.getNextPaymentDate() == null || !sub.getNextPaymentDate().isAfter(now)) {
                markExpired(sub);
                expiredCancelled++;
            }
        }

        if (expiredPastDue > 0 || expiredCancelled > 0) {
            log.info("Subscription reconcile expired {} past-due and {} cancelled subscriptions.",
                    expiredPastDue, expiredCancelled);
        }
    }

    private void markExpired(MemberSubscription sub) {
        sub.setStatus("expired");
        memberSubscriptionRepository.save(sub);
        User user = sub.getUserId() != null ? userRepository.findById(sub.getUserId()).orElse(null) : null;
        if (user != null) {
            log.warn("Subscription expired for member {} ({}); shown inactive in admin portal.",
                    user.getEmail(), sub.getSubscriptionCode());
        } else {
            log.warn("Subscription expired for user id {}; shown inactive in admin portal.", sub.getUserId());
        }
    }
}