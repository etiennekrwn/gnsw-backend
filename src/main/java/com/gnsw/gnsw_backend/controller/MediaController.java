package com.gnsw.gnsw_backend.controller;

import com.gnsw.gnsw_backend.dto.response.ApiResponse;
import com.gnsw.gnsw_backend.entity.MediaFile;
import com.gnsw.gnsw_backend.service.MediaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/media")
@RequiredArgsConstructor
@Slf4j
public class MediaController {

    private final MediaService mediaService;

    /**
     * Upload one or more files.
     * POST /api/v1/media/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> uploadFiles(
            @RequestParam("files") List<MultipartFile> files,
            Authentication authentication) {
        try {
            String username = authentication.getName();
            // Get user ID from authentication - using Username as identifier
            // In a real app, you'd get the full user details
            UUID uploaderId = UUID.nameUUIDFromBytes(username.getBytes());
            
            List<MediaFile> uploadedFiles = mediaService.uploadFiles(files, uploaderId, "USER");

            List<Map<String, Object>> mediaResponses = uploadedFiles.stream()
                    .map(this::convertToMap)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                    .success(true)
                    .message("Uploaded " + uploadedFiles.size() + " file(s) successfully")
                    .data(mediaResponses)
                    .build());

        } catch (Exception e) {
            log.error("File upload failed: {}", e.getMessage(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<List<Map<String, Object>>>builder()
                            .success(false)
                            .message("Upload failed: " + e.getMessage())
                            .build());
        }
    }

    /**
     * List all media files (admin only).
     * GET /api/v1/media
     */
    @GetMapping
    @PreAuthorize("hasAuthority('MODULE_MEDIA')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllMedia() {
        List<MediaFile> mediaFiles = mediaService.getAllMedia();
        List<Map<String, Object>> mediaResponses = mediaFiles.stream()
                .map(this::convertToMap)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.<List<Map<String, Object>>>builder()
                .success(true)
                .message("Retrieved " + mediaFiles.size() + " media files")
                .data(mediaResponses)
                .build());
    }

    /**
     * Delete a media file.
     * DELETE /api/v1/media/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(
            @PathVariable UUID id,
            Authentication authentication) {
        String username = authentication.getName();
        UUID userId = UUID.nameUUIDFromBytes(username.getBytes());
        
        boolean deleted = mediaService.deleteMedia(id, userId);
        
        if (deleted) {
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .success(true)
                    .message("Media file deleted successfully")
                    .build());
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.<Void>builder()
                            .success(false)
                            .message("File not found or access denied")
                            .build());
        }
    }

         /**
     * Convert MediaFile entity to response map.
     */
    private Map<String, Object> convertToMap(MediaFile mediaFile) {
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("id", mediaFile.getId());
        result.put("name", mediaFile.getOriginalName() != null ? mediaFile.getOriginalName() : mediaFile.getFileName());
        result.put("url", mediaFile.getFileUrl());
        result.put("thumbnailUrl", mediaFile.getThumbnailUrl());
        result.put("type", mediaFile.getContentType() != null ? mediaFile.getContentType().split("/")[0] : "file");
        result.put("fileType", mediaFile.getFileType());
        result.put("contentType", mediaFile.getContentType());
        result.put("size", MediaService.formatFileSize(mediaFile.getFileSize() != null ? mediaFile.getFileSize() : 0));
        result.put("sizeBytes", mediaFile.getFileSize() != null ? mediaFile.getFileSize() : 0);
        result.put("isImage", mediaFile.getIsImage() != null && mediaFile.getIsImage());
        result.put("createdAt", mediaFile.getCreatedAt());
        result.put("uploaderId", mediaFile.getUploaderId());
        return result;
    }
}