package com.gnsw.gnsw_backend.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Public-facing member profile data for the website directory.
 * Contains ONLY fields that should be visible to the public.
 */
@Data
@Builder
public class DirectoryMemberResponse {
    private String id;
    private String firstName;
    private String lastName;
    private String city;
    private String stateProvince;
    private String country;
    private String tier;               // e.g. "Affiliate", "Associate", "Member", "Fellow"
    private String professionalId;
    private String verificationDate;   // ISO date time (approvedAt)
    private String currentProfessionalRole;
    private String favouriteOrator;
    private String zone;
    private String profileImageUrl;
    private String organisation;
    private String bio;
    private String headline;           // short summary line (first ~120 chars of bio)
    private boolean verified;
    private String socialMediaPlatform;  // detail view only
    private String currentJobTitle;      // detail view only
    private String currentOrganization;  // detail view only
}