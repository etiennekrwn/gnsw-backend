package com.gnsw.gnsw_backend.service;

import com.gnsw.gnsw_backend.entity.User;
import com.gnsw.gnsw_backend.enums.UserStatus;
import com.gnsw.gnsw_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Automatic fail-safe for paying members who never clicked their activation
 * link before it expired. Runs daily and re-issues a fresh 48h link for any
 * accepted member who still hasn't set a password, up to {@value #MAX_REMINDERS}
 * consecutive reminders so we never spam an abandoned account forever.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ActivationReminderJob {

    private static final int MAX_REMINDERS = 3;

    private final UserRepository userRepository;
    private final EmailService emailService;

    @Scheduled(cron = "${app.activation-reminder-cron:0 0 6 * * *}", zone = "Africa/Lagos")
    public void reissueExpiredActivationLinks() {
        LocalDateTime now = LocalDateTime.now();

        List<User> expired = userRepository
                .findByStatusAndPasswordSetAtIsNullAndPasswordSetTokenExpiresAtBefore(
                        UserStatus.ACCEPTED, now);

        int sent = 0;
        for (User user : expired) {
            int count = user.getActivationReminderCount() == null
                    ? 0
                    : user.getActivationReminderCount();

            if (count >= MAX_REMINDERS) {
                continue;
            }

            String token = UUID.randomUUID().toString();
            user.setPasswordSetToken(token);
            user.setPasswordSetTokenExpiresAt(now.plusHours(48));
            user.setActivationReminderCount(count + 1);
            userRepository.save(user);

            try {
                emailService.sendApprovalEmail(
                        user.getEmail(),
                        user.getFirstName(),
                        user.getTier() != null ? user.getTier().name() : "MEMBER",
                        user.getProfessionalId(),
                        token);
                sent++;
            } catch (Exception e) {
                log.error("Failed to send activation reminder to {}: {}",
                        user.getEmail(), e.getMessage());
            }
        }

        if (sent > 0) {
            log.info("Activation reminder job re-issued {} expired activation link(s).", sent);
        }
    }
}