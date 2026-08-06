package com.gnsw.gnsw_backend.service;

import com.gnsw.gnsw_backend.entity.Payment;
import com.gnsw.gnsw_backend.entity.User;
import com.gnsw.gnsw_backend.enums.PaymentStatus;
import com.gnsw.gnsw_backend.repository.ApplicationRepository;
import com.gnsw.gnsw_backend.repository.PaymentRepository;
import com.gnsw.gnsw_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final EmailService emailService;

    @Value("${paystack.secret-key}")
    private String paystackSecretKey;

    public Map<String, Object> initializePayment(String applicationId, String email, int amount, String reference) {
        User user = userRepository.findById(UUID.fromString(applicationId))
                .orElseThrow(() -> new IllegalArgumentException("Application not found."));

        if (user.getEmailVerifiedAt() == null) {
            throw new IllegalArgumentException("Email must be verified before payment.");
        }

        if (paymentRepository.existsByUserIdAndStatus(user.getId(), PaymentStatus.SUCCESS)) {
            throw new IllegalArgumentException("Payment has already been made for this application.");
        }

        // Create payment record
        Payment payment = Payment.builder()
                .userId(user.getId())
                .reference(reference)
                .amount(amount)
                .tier(user.getTier())
                .status(PaymentStatus.PENDING)
                .build();
        paymentRepository.save(payment);

        // Call Paystack API to initialize transaction
        try {
            HttpClient client = HttpClient.newHttpClient();
            String json = String.format(
                    "{\"email\":\"%s\",\"amount\":%d,\"reference\":\"%s\"}",
                    email, amount, reference
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.paystack.co/transaction/initialize"))
                    .header("Authorization", "Bearer " + paystackSecretKey)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Parse response
            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(response.body(), Map.class);

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

            return Map.of(
                    "authorizationUrl", data.get("authorization_url"),
                    "accessCode", data.get("access_code"),
                    "reference", reference
            );
        } catch (Exception e) {
            log.error("Paystack initialization failed: {}", e.getMessage());
            throw new RuntimeException("Payment initialization failed. Please try again.");
        }
    }

    public Map<String, Object> verifyPayment(String reference, String applicationId) {
        Payment payment = paymentRepository.findByReference(reference)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found."));

        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return Map.of("status", "SUCCESS", "message", "Payment already verified.");
        }

        // Call Paystack to verify
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.paystack.co/transaction/verify/" + reference))
                    .header("Authorization", "Bearer " + paystackSecretKey)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(response.body(), Map.class);

            if (Boolean.TRUE.equals(responseBody.get("status"))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

                if ("success".equals(data.get("status"))) {
                    payment.setStatus(PaymentStatus.SUCCESS);
                    payment.setPaidAt(LocalDateTime.now());
                    payment.setPaystackResponse(response.body());
                    paymentRepository.save(payment);

                    // Send payment confirmation email
                    User user = userRepository.findById(payment.getUserId()).orElse(null);
                    if (user != null) {
                        emailService.sendPaymentConfirmation(
                                user.getEmail(),
                                payment.getTier().name(),
                                String.valueOf(payment.getAmount())
                        );
                    }

                    return Map.of("status", "SUCCESS", "message", "Payment verified successfully.");
                }
            }

            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            return Map.of("status", "FAILED", "message", "Payment verification failed.");

        } catch (Exception e) {
            log.error("Paystack verification failed: {}", e.getMessage());
            throw new RuntimeException("Payment verification failed. Please try again.");
        }
    }

    /**
     * Verify a payment by reference only (without requiring a Payment record).
     * Used by NewApplicationService to verify Paystack payments before creating applications.
     * Throws an exception if verification fails or amount doesn't match.
     */
    public void verifyPaymentByReference(String reference, int expectedAmount) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.paystack.co/transaction/verify/" + reference))
                    .header("Authorization", "Bearer " + paystackSecretKey)
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            @SuppressWarnings("unchecked")
            Map<String, Object> responseBody = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(response.body(), Map.class);

            if (!Boolean.TRUE.equals(responseBody.get("status"))) {
                throw new IllegalArgumentException("Payment verification failed with Paystack.");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) responseBody.get("data");

            if (!"success".equals(data.get("status"))) {
                throw new IllegalArgumentException("Payment was not successful.");
            }

            // Verify amount matches
            int paystackAmount = (Integer) data.get("amount");
            if (paystackAmount != expectedAmount) {
                throw new IllegalArgumentException("Payment amount mismatch. Expected: " + expectedAmount + ", Got: " + paystackAmount);
            }

            log.info("Payment verified successfully for reference: {}", reference);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Paystack verification failed for reference {}: {}", reference, e.getMessage());
            throw new RuntimeException("Payment verification failed: " + e.getMessage());
        }
    }

    private final com.gnsw.gnsw_backend.repository.MemberSubscriptionRepository memberSubscriptionRepository;

    public void handleWebhook(String payload, String signature) {
        // Verify HMAC signature
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA512");
            javax.crypto.spec.SecretKeySpec secretKeySpec =
                    new javax.crypto.spec.SecretKeySpec(paystackSecretKey.getBytes(), "HmacSHA512");
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(payload.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            String computedSignature = hexString.toString();

            if (!computedSignature.equals(signature)) {
                log.warn("Invalid webhook signature");
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> payloadMap = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(payload, Map.class);

            String event = (String) payloadMap.get("event");
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) payloadMap.get("data");

            if ("charge.success".equals(event)) {
                String reference = (String) data.get("reference");
                Payment payment = paymentRepository.findByReference(reference).orElse(null);
                if (payment != null && payment.getStatus() != PaymentStatus.SUCCESS) {
                    payment.setStatus(PaymentStatus.SUCCESS);
                    payment.setPaidAt(LocalDateTime.now());
                    payment.setPaystackResponse(payload);
                    paymentRepository.save(payment);

                    User user = userRepository.findById(payment.getUserId()).orElse(null);
                    if (user != null) {
                        emailService.sendPaymentConfirmation(
                                user.getEmail(),
                                payment.getTier().name(),
                                String.valueOf(payment.getAmount())
                        );
                    }
                }
            } else if ("subscription.create".equals(event)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> customer = (Map<String, Object>) data.get("customer");
                String email = (String) customer.get("email");

                @SuppressWarnings("unchecked")
                Map<String, Object> plan = (Map<String, Object>) data.get("plan");
                String planCode = plan != null ? (String) plan.get("plan_code") : "UNKNOWN";
                String subscriptionCode = (String) data.get("subscription_code");
                String emailToken = (String) data.get("email_token");

                // At checkout time the Application exists but the User does NOT yet
                // (users are created at approval). Capture the subscription on the
                // Application so it survives until approval, and on the User for renewals.
                com.gnsw.gnsw_backend.entity.Application application =
                        applicationRepository.findByEmail(email).orElse(null);
                if (application != null) {
                    application.setSubscriptionCode(subscriptionCode);
                    application.setPlanCode(planCode);
                    application.setEmailToken(emailToken);
                    application.setPaymentStatus("SUCCESS");
                    applicationRepository.save(application);
                    log.info("Subscription stored on application for email: {}", email);
                }

                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null) {
                    com.gnsw.gnsw_backend.entity.MemberSubscription sub = memberSubscriptionRepository.findByUserId(user.getId())
                            .orElse(new com.gnsw.gnsw_backend.entity.MemberSubscription());
                    sub.setUserId(user.getId());
                    sub.setSubscriptionCode(subscriptionCode);
                    sub.setPlanCode(planCode);
                    sub.setEmailToken(emailToken);
                    sub.setStatus("active");
                    sub.setNextPaymentDate(LocalDateTime.now().plusYears(1));

                    memberSubscriptionRepository.save(sub);
                    log.info("Subscription created for user: {}", email);
                }

                if (application == null && user == null) {
                    log.warn("No application or user found for subscription email: {}", email);
                }
            } else if ("invoice.update".equals(event) || "invoice.create".equals(event)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> subscription = (Map<String, Object>) data.get("subscription");
                if (subscription != null) {
                    String subCode = (String) subscription.get("subscription_code");
                    memberSubscriptionRepository.findBySubscriptionCode(subCode).ifPresent(sub -> {
                        if ("success".equals(data.get("status"))) {
                            sub.setStatus("active");
                            sub.setNextPaymentDate(LocalDateTime.now().plusYears(1));
                            memberSubscriptionRepository.save(sub);
                            log.info("Subscription renewed for code: {}", subCode);
                        }
                    });
                }
            } else if ("invoice.payment_failed".equals(event)) {
                @SuppressWarnings("unchecked")
                Map<String, Object> subscription = (Map<String, Object>) data.get("subscription");
                if (subscription != null) {
                    String subCode = (String) subscription.get("subscription_code");
                    memberSubscriptionRepository.findBySubscriptionCode(subCode).ifPresent(sub -> {
                        sub.setStatus("past_due");
                        memberSubscriptionRepository.save(sub);
                        log.warn("Subscription payment failed for code: {}", subCode);
                    });
                }
            } else if ("subscription.disable".equals(event)) {
                String subCode = (String) data.get("subscription_code");
                memberSubscriptionRepository.findBySubscriptionCode(subCode).ifPresent(sub -> {
                    sub.setStatus("cancelled");
                    memberSubscriptionRepository.save(sub);
                    log.info("Subscription cancelled for code: {}", subCode);
                });
            }

        } catch (Exception e) {
            log.error("Webhook processing failed: {}", e.getMessage());
        }
    }
}