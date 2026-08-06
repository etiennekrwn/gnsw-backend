package com.gnsw.gnsw_backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ApplicationRequest {
    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Address is required")
    private String addressLine1;

    private String addressLine2;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State/Province is required")
    private String stateProvince;

    @NotBlank(message = "Zip/Postal code is required")
    private String zipPostalCode;

    @NotBlank(message = "Country is required")
    private String country;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Membership tier is required")
    @Pattern(regexp = "AFFILIATE|ASSOCIATE|MEMBER", message = "Tier must be AFFILIATE, ASSOCIATE, or MEMBER")
    private String membershipTier;
}