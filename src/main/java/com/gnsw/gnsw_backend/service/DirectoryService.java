package com.gnsw.gnsw_backend.service;

import com.gnsw.gnsw_backend.dto.response.DirectoryMemberResponse;
import com.gnsw.gnsw_backend.entity.DirectoryContactMessage;
import com.gnsw.gnsw_backend.entity.Member;
import com.gnsw.gnsw_backend.entity.User;
import com.gnsw.gnsw_backend.enums.UserStatus;
import com.gnsw.gnsw_backend.repository.DirectoryContactMessageRepository;
import com.gnsw.gnsw_backend.repository.MemberRepository;
import com.gnsw.gnsw_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Public members directory — combines User (identity/tier) + Member (profile) data.
 */
@Service
@RequiredArgsConstructor
public class DirectoryService {

    private final UserRepository userRepository;
    private final MemberRepository memberRepository;
    private final DirectoryContactMessageRepository contactMessageRepository;

    /**
     * List all approved members (status = ACCEPTED), optionally filtered.
     */
    public List<DirectoryMemberResponse> listDirectory(String tier, String zone, String speechType, String search) {
        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.ASC, "lastName"));

        return users.stream()
                .filter(user -> user.getStatus() == UserStatus.ACCEPTED)
                .filter(user -> !"ROLE_ADMIN".equals(user.getRole()))
                .filter(user -> filterByTier(user, tier))
                .filter(user -> filterByZone(user, zone))
                .filter(user -> filterBySpeechType(user, speechType))
                .filter(user -> filterBySearch(user, search))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a single public member profile by user id.
     */
    public DirectoryMemberResponse getDirectoryMember(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        if (user.getStatus() != UserStatus.ACCEPTED) {
            throw new IllegalArgumentException("Member not found.");
        }
        return toResponse(user);
    }

    /**
     * Save a contact message from the directory "Contact" modal.
     */
    public void submitContact(UUID memberId, String senderName, String senderEmail, String message) {
        // Ensure the member exists and is accepted before saving a message
        User user = userRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found."));
        if (user.getStatus() != UserStatus.ACCEPTED) {
            throw new IllegalArgumentException("Member not found.");
        }

        DirectoryContactMessage contact = DirectoryContactMessage.builder()
                .memberId(memberId)
                .senderName(senderName)
                .senderEmail(senderEmail)
                .message(message)
                .isRead(false)
                .build();
        contactMessageRepository.save(contact);
    }

    // --- Filter helpers ---

    private boolean filterByTier(User user, String tier) {
        if (tier == null || tier.isBlank() || "All".equalsIgnoreCase(tier)) return true;
        return user.getTier() != null && user.getTier().name().equalsIgnoreCase(tier);
    }

    private boolean filterByZone(User user, String zone) {
        if (zone == null || zone.isBlank() || "All".equalsIgnoreCase(zone)) return true;
        Member member = memberRepository.findByUserId(user.getId()).orElse(null);
        return member != null && zone.equalsIgnoreCase(member.getZone());
    }

    private boolean filterBySpeechType(User user, String speechType) {
        if (speechType == null || speechType.isBlank() || "All".equalsIgnoreCase(speechType)) return true;
        Member member = memberRepository.findByUserId(user.getId()).orElse(null);
        if (member == null || member.getCurrentProfessionalRole() == null) return false;
        return member.getCurrentProfessionalRole().equalsIgnoreCase(speechType);
    }

    private boolean filterBySearch(User user, String search) {
        if (search == null || search.isBlank()) return true;
        String query = search.trim().toLowerCase(Locale.ROOT);
        Member member = memberRepository.findByUserId(user.getId()).orElse(null);

        boolean nameMatch = (user.getFirstName() != null && user.getFirstName().toLowerCase(Locale.ROOT).contains(query))
                || (user.getLastName() != null && user.getLastName().toLowerCase(Locale.ROOT).contains(query));
        boolean cityMatch = user.getCity() != null && user.getCity().toLowerCase(Locale.ROOT).contains(query);
        boolean tierMatch = user.getTier() != null && user.getTier().name().toLowerCase(Locale.ROOT).contains(query);
        boolean roleMatch = member != null && member.getCurrentProfessionalRole() != null
                && member.getCurrentProfessionalRole().toLowerCase(Locale.ROOT).contains(query);
        boolean jobMatch = member != null && member.getCurrentJobTitle() != null
                && member.getCurrentJobTitle().toLowerCase(Locale.ROOT).contains(query);

        return nameMatch || cityMatch || tierMatch || roleMatch || jobMatch;
    }

    // --- Mapping helpers ---

    private DirectoryMemberResponse toResponse(User user) {
        Member member = memberRepository.findByUserId(user.getId()).orElse(null);

        String bio = member != null ? member.getBio() : null;
        String headline = null;
        if (bio != null && !bio.isBlank()) {
            headline = bio.length() > 120 ? bio.substring(0, 120) + "..." : bio;
        } else if (member != null && member.getOrganisation() != null) {
            headline = member.getOrganisation();
        }

        return DirectoryMemberResponse.builder()
                .id(user.getId().toString())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .city(user.getCity())
                .stateProvince(user.getStateProvince())
                .country(user.getCountry())
                .tier(user.getTier() != null ? titleCase(user.getTier().name()) : null)
                .professionalId(user.getProfessionalId())
                .verificationDate(user.getApprovedAt() != null ? user.getApprovedAt().toString() : null)
                .currentProfessionalRole(member != null ? member.getCurrentProfessionalRole() : null)
                .favouriteOrator(member != null ? member.getFavouriteOrator() : null)
                .zone(member != null ? member.getZone() : null)
                .profileImageUrl(member != null ? member.getProfileImageUrl() : null)
                .organisation(member != null ? member.getOrganisation() : null)
                .bio(bio)
                .headline(headline)
                .verified(member != null && member.isVerified())
                .socialMediaPlatform(member != null ? member.getSocialMediaPlatform() : null)
                .currentJobTitle(member != null ? member.getCurrentJobTitle() : null)
                .currentOrganization(member != null ? member.getCurrentOrganization() : null)
                .build();
    }

    private List<String> splitCsv(String csv) {
        if (csv == null || csv.isBlank()) return Collections.emptyList();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private String titleCase(String value) {
        if (value == null || value.isEmpty()) return value;
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1).toLowerCase(Locale.ROOT);
    }
}