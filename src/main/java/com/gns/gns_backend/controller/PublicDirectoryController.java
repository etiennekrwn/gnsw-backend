package com.gns.gns_backend.controller;

import com.gns.gns_backend.dto.request.DirectoryContactRequest;
import com.gns.gns_backend.dto.response.ApiResponse;
import com.gns.gns_backend.dto.response.DirectoryMemberResponse;
import com.gns.gns_backend.service.DirectoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Public members directory — no authentication required.
 */
@RestController
@RequestMapping("/api/v1/public/directory")
@RequiredArgsConstructor
public class PublicDirectoryController {

    private final DirectoryService directoryService;

    /**
     * List all approved members with optional filters.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<DirectoryMemberResponse>>> listDirectory(
            @RequestParam(required = false) String tier,
            @RequestParam(required = false) String zone,
            @RequestParam(required = false) String speechType,
            @RequestParam(required = false) String search) {
        List<DirectoryMemberResponse> members =
                directoryService.listDirectory(tier, zone, speechType, search);
        return ResponseEntity.ok()
                .body(ApiResponse.<List<DirectoryMemberResponse>>builder()
                        .success(true)
                        .message("Directory retrieved.")
                        .data(members)
                        .build());
    }

    /**
     * Get a single member's public profile.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DirectoryMemberResponse>> getMember(@PathVariable UUID id) {
        DirectoryMemberResponse member = directoryService.getDirectoryMember(id);
        return ResponseEntity.ok()
                .body(ApiResponse.<DirectoryMemberResponse>builder()
                        .success(true)
                        .message("Member profile retrieved.")
                        .data(member)
                        .build());
    }

    /**
     * Submit a contact message to a member from the directory.
     */
    @PostMapping("/{id}/contact")
    public ResponseEntity<ApiResponse<Map<String, String>>> submitContact(
            @PathVariable UUID id,
            @Valid @RequestBody DirectoryContactRequest request) {
        directoryService.submitContact(id, request.getName(), request.getEmail(), request.getMessage());
        return ResponseEntity.ok()
                .body(ApiResponse.<Map<String, String>>builder()
                        .success(true)
                        .message("Message sent successfully.")
                        .data(Map.of("status", "sent"))
                        .build());
    }
}