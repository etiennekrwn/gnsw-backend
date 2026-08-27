package com.gns.gns_backend.controller;

import com.gns.gns_backend.dto.request.PaymentInitRequest;
import com.gns.gns_backend.dto.request.PaymentVerifyRequest;
import com.gns.gns_backend.dto.response.ApiResponse;
import com.gns.gns_backend.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public/payments")
@RequiredArgsConstructor
public class PublicPaymentController {

    private final PaymentService paymentService;

    @PostMapping("/initialize")
    public ResponseEntity<ApiResponse<Map<String, Object>>> initializePayment(
            @Valid @RequestBody PaymentInitRequest request) {
        Map<String, Object> result = paymentService.initializePayment(
                request.getApplicationId(),
                request.getEmail(),
                request.getAmount(),
                request.getReference()
        );
        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Payment initialized.")
                        .data(result)
                        .build());
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyPayment(
            @Valid @RequestBody PaymentVerifyRequest request) {
        Map<String, Object> result = paymentService.verifyPayment(
                request.getReference(),
                request.getApplicationId()
        );
        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message((String) result.get("message"))
                        .data(result)
                        .build());
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("x-paystack-signature") String signature) {
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok("OK");
    }
}