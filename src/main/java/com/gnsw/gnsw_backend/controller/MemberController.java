package com.gnsw.gnsw_backend.controller;

import com.gnsw.gnsw_backend.dto.response.ApiResponse;
import com.gnsw.gnsw_backend.entity.Member;
import com.gnsw.gnsw_backend.entity.User;
import com.gnsw.gnsw_backend.repository.MemberRepository;
import com.gnsw.gnsw_backend.repository.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final UserRepository userRepository;
    private final MemberRepository memberRepository;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(Authentication authentication) {
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        Member member = memberRepository.findByUserId(user.getId()).orElse(null);

        Map<String, Object> profile = new HashMap<>();
        profile.put("id", user.getId());
        profile.put("email", user.getEmail());
        profile.put("firstName", user.getFirstName());
        profile.put("lastName", user.getLastName());
        profile.put("tier", user.getTier() != null ? user.getTier().name() : null);
        profile.put("role", user.getRole());
        profile.put("professionalId", user.getProfessionalId());
        profile.put("status", user.getStatus() != null ? user.getStatus().name() : null);

        if (member != null) {
            profile.put("profileImageUrl", member.getProfileImageUrl());
            profile.put("organisation", member.getOrganisation());
            profile.put("bio", member.getBio());
            profile.put("phone", member.getPhone());
            profile.put("sectors", member.getSectors());
            profile.put("speechTypes", member.getSpeechTypes());
            profile.put("languages", member.getLanguages());
            profile.put("zone", member.getZone());
            profile.put("linkedInProfile", member.getLinkedInProfile());
            profile.put("socials", member.getSocials());
            profile.put("reasonForJoining", member.getReasonForJoining());
            profile.put("isVerified", member.isVerified());
            profile.put("memberSince", member.getCreatedAt());
        }

        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Profile retrieved.")
                        .data(profile)
                        .build());
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        String username = authentication.getName();

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));

        Member member = memberRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Member newMember = Member.builder()
                            .userId(user.getId())
                            .build();
                    return memberRepository.save(newMember);
                });

        // Update member fields
        if (request.getOrganisation() != null) {
            member.setOrganisation(request.getOrganisation());
        }
        if (request.getBio() != null) {
            member.setBio(request.getBio());
        }
        if (request.getPhone() != null) {
            member.setPhone(request.getPhone());
        }
        if (request.getSectors() != null) {
            member.setSectors(request.getSectors());
        }
        if (request.getSpeechTypes() != null) {
            member.setSpeechTypes(request.getSpeechTypes());
        }
        if (request.getLanguages() != null) {
            member.setLanguages(request.getLanguages());
        }
        if (request.getZone() != null) {
            member.setZone(request.getZone());
        }
        if (request.getProfileImageUrl() != null) {
            member.setProfileImageUrl(request.getProfileImageUrl());
        }
        if (request.getLinkedInProfile() != null) {
            member.setLinkedInProfile(request.getLinkedInProfile());
        }
        if (request.getSocials() != null) {
            member.setSocials(request.getSocials());
        }

        memberRepository.save(member);

        // Build response
        Map<String, Object> profile = new HashMap<>();
        profile.put("organisation", member.getOrganisation());
        profile.put("bio", member.getBio());
        profile.put("phone", member.getPhone());
        profile.put("sectors", member.getSectors());
        profile.put("speechTypes", member.getSpeechTypes());
        profile.put("languages", member.getLanguages());
        profile.put("zone", member.getZone());
        profile.put("profileImageUrl", member.getProfileImageUrl());
        profile.put("linkedInProfile", member.getLinkedInProfile());
        profile.put("socials", member.getSocials());

        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, Object>>builder()
                        .success(true)
                        .message("Profile updated successfully.")
                        .data(profile)
                        .build());
    }

    @Data
    public static class UpdateProfileRequest {
        @Size(max = 255)
        private String organisation;

        @Size(max = 2000)
        private String bio;

        private String phone;

        @Size(max = 1000)
        private String sectors;

        @Size(max = 1000)
        private String speechTypes;

        @Size(max = 500)
        private String languages;

        private String zone;

        private String profileImageUrl;

        @Size(max = 500)
        private String linkedInProfile;

        @Size(max = 2000)
        private String socials;
    }
}