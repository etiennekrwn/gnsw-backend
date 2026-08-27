package com.gnsw.gnsw_backend.service;

import com.gnsw.gnsw_backend.event.ApplicationApprovedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends the welcome/activation email after the approval transaction commits.
 * Runs asynchronously so a slow or failing email never delays or breaks the
 * approval response; failures are logged and swallowed.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationApprovalListener {

    private final EmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onApplicationApproved(ApplicationApprovedEvent event) {
        try {
            emailService.sendApprovalEmail(
                    event.email(),
                    event.firstName(),
                    event.tier(),
                    event.professionalId(),
                    event.token(),
                    event.customMessage());
        } catch (Exception e) {
            log.error("Failed to send approval email to {}: {}", event.email(), e.getMessage());
        }
    }
}
