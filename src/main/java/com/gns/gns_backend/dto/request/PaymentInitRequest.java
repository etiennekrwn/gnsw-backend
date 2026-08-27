package com.gns.gns_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentInitRequest {
    @NotBlank(message = "Application ID is required")
    private String applicationId;

    @NotBlank(message = "Email is required")
    private String email;

    @NotNull(message = "Amount is required")
    private Integer amount;

    @NotBlank(message = "Reference is required")
    private String reference;
}