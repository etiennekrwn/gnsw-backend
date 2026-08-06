package com.gnsw.gnsw_backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectApplicationRequest {
    @NotBlank(message = "Rejection reason is required")
    private String reason;
}