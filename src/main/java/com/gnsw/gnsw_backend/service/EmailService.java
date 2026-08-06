package com.gnsw.gnsw_backend.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend.member-portal-url}")
    private String memberPortalUrl;

    public void sendOtpEmail(String to, String otpCode) {
        sendEmail(to, "Verify Your Email — GNSW Membership Application",
                "otp-email", "otpCode", otpCode);
    }

    public void sendPaymentConfirmation(String to, String tier, String amount) {
        Context context = new Context();
        context.setVariable("tier", tier);
        context.setVariable("amount", amount);
        String html = templateEngine.process("payment-confirmation", context);
        sendHtmlEmail(to, "Payment Received — GNSW Application Under Review", html);
    }

    public void sendApprovalEmail(String to, String firstName, String tier,
                                   String professionalId, String token) {
        Context context = new Context();
        context.setVariable("firstName", firstName);
        context.setVariable("tier", tier);
        context.setVariable("professionalId", professionalId);
        context.setVariable("setPasswordUrl", memberPortalUrl + "/set-password?token=" + token + "&email=" + to);
        String html = templateEngine.process("approval-email", context);
        sendHtmlEmail(to, "Welcome to the Guild of Nigerian Speechwriters! 🎉", html);
    }

    public void sendRejectionEmail(String to, String firstName, String reason) {
        Context context = new Context();
        context.setVariable("firstName", firstName);
        context.setVariable("reason", reason);
        String html = templateEngine.process("rejection-email", context);
        sendHtmlEmail(to, "Update on Your GNSW Membership Application", html);
    }

    public void sendPasswordSetConfirmation(String to, String username) {
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("loginUrl", memberPortalUrl + "/login");
        String html = templateEngine.process("password-set-confirmation", context);
        sendHtmlEmail(to, "Your GNSW Account is Now Active", html);
    }

    private void sendEmail(String to, String subject, String template,
                            String variableName, String variableValue) {
        try {
            Context context = new Context();
            context.setVariable(variableName, variableValue);
            String html = templateEngine.process(template, context);
            sendHtmlEmail(to, subject, html);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}