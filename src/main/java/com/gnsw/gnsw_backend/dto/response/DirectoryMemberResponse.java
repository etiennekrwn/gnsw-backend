package com.gnsw.gnsw_backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

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
    private List<String> speechTypes;
    private List<String> sectors;
    private List<String> languages;
    private String zone;
    private String profileImageUrl;
    private String organisation;
    private String bio;
    private String headline;           // short summary line (first ~120 chars of bio)
    private boolean verified;
    private String linkedInProfile;    // detail view only
    private String socials;            // detail view only
}