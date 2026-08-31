package com.gnsw.gnsw_backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class EmailService {

    private final TemplateEngine templateEngine;

    // Railway blocks outbound SMTP on free/hobby plans, so we send via
    // Brevo's HTTPS REST API (port 443) which is NOT blocked.
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.mail.brevo-api-key:}")
    private String brevoApiKey;

    @Value("${app.mail.sender-email:}")
    private String fromEmail;

    @Value("${app.mail.sender-name:GNSW}")
    private String fromName;

    @Value("${app.frontend.member-portal-url}")
    private String memberPortalUrl;

    @Value("${app.frontend.admin-url}")
    private String adminUrl;

    public EmailService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendOtpEmail(String to, String otpCode) {
        sendEmail(to, "Verify Your Email — GNSW Membership Application",
                "otp-email", "otpCode", otpCode);
    }

    @Async
    public void sendPaymentConfirmation(String to, String tier, String amount) {
        Context context = new Context();
        context.setVariable("tier", tier);
        context.setVariable("amount", amount);
        String html = templateEngine.process("payment-confirmation", context);
        sendHtmlEmail(to, "Payment Received — GNSW Application Under Review", html);
    }

    @Async
    public void sendApplicationReceivedEmail(String to, String firstName, String tier,
                                             String applicationId) {
        Context context = new Context();
        context.setVariable("firstName", firstName);
        context.setVariable("tier", tier);
        context.setVariable("applicationId", applicationId);
        String html = templateEngine.process("application-received", context);
        sendHtmlEmail(to, "Your GNSW Application Has Been Received", html);
    }

    @Async
    public void sendApprovalEmail(String to, String firstName, String tier,
                                   String professionalId, String token,
                                   String customMessage, String annualFee) {
        Context context = new Context();
        context.setVariable("firstName", firstName);
        context.setVariable("tier", tier);
        context.setVariable("professionalId", professionalId);
        context.setVariable("annualFee", annualFee);
        context.setVariable("setPasswordUrl", memberPortalUrl + "/set-password?token=" + token + "&email=" + to);
        context.setVariable("customMessage", customMessage);
        String html = templateEngine.process("approval-email", context);
        sendHtmlEmail(to, "Welcome to the Guild of Nigerian Speechwriters! 🎉", html);
    }

    @Async
    public void sendRejectionEmail(String to, String firstName, String reason) {
        Context context = new Context();
        context.setVariable("firstName", firstName);
        context.setVariable("reason", reason);
        String html = templateEngine.process("rejection-email", context);
        sendHtmlEmail(to, "Update on Your GNSW Membership Application", html);
    }

    @Async
    public void sendPasswordSetConfirmation(String to, String username) {
        Context context = new Context();
        context.setVariable("username", username);
        context.setVariable("loginUrl", memberPortalUrl + "/login");
        String html = templateEngine.process("password-set-confirmation", context);
        sendHtmlEmail(to, "Your GNSW Account is Now Active", html);
    }

    @Async
    public void sendPasswordResetEmail(String to, String firstName, String token) {
        Context context = new Context();
        context.setVariable("firstName", firstName);
        context.setVariable("resetUrl", memberPortalUrl + "/reset-password?token=" + token);
        String html = templateEngine.process("reset-password", context);
        sendHtmlEmail(to, "Reset Your GNSW Password", html);
    }

    /**
     * Onboarding email for a newly invited admin-console account. The recipient
     * sets their own password via the one-time token entered below.
     */
    @Async
    public void sendAdminInvite(String to, String displayName, String roleLabel,
                                String modulesLabel, String token) {
        Context context = new Context();
        context.setVariable("displayName", displayName);
        context.setVariable("roleLabel", roleLabel);
        context.setVariable("modulesLabel", modulesLabel == null ? "" : modulesLabel);
        context.setVariable("acceptUrl", adminUrl + "/accept-invite?token=" + token + "&email=" + to);
        String html = templateEngine.process("admin-invite", context);
        sendHtmlEmail(to, "You've been invited to the GNSW Admin Console", html);
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

    /**
     * Sends email via Brevo's HTTPS REST API (port 443).
     * Railway blocks outbound SMTP on free/hobby plans, so SMTP (Gmail/Zoho/Brevo-SMTP)
     * all time out. The HTTPS API is NOT blocked and works from Railway.
     * Failures are caught and logged so they never propagate to the API caller.
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            log.error("BREVO_API_KEY not configured. Cannot send email to {}.", to);
            return;
        }
        try {
            Map<String, Object> payload = Map.of(
                    "sender", Map.of("name", fromName, "email", fromEmail),
                    "to", List.of(Map.of("email", to)),
                    "subject", subject,
                    "htmlContent", htmlContent
            );

            String json = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.brevo.com/v3/smtp/email"))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .header("api-key", brevoApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("Email sent to {}: {} (Brevo status {})", to, subject, response.statusCode());
            } else {
                log.error("Brevo send failed to {}: status {} body {}", to, response.statusCode(), response.body());
            }
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}
