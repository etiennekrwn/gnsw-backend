package com.gns.gns_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentVerifyRequest {
    @NotBlank(message = "Reference is required")
    private String reference;

    @NotBlank(message = "Application ID is required")
    private String applicationId;

    private String trxref;
}