package com.gnsw.gnsw_backend.service;

import com.gnsw.gnsw_backend.entity.MediaFile;
import com.gnsw.gnsw_backend.repository.MediaFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final MediaFileRepository mediaFileRepository;
    private final StorageService storageService;

    @Value("${app.frontend.member-portal-url:http://localhost:5175}")
    private String memberPortalUrl;

    /**
     * Upload one or more files to cloud storage (Supabase Storage) and persist metadata to DB.
     */
    public List<MediaFile> uploadFiles(List<MultipartFile> files, UUID uploaderId, String uploaderRole) throws IOException {
        List<MediaFile> savedFiles = new java.util.ArrayList<>();

        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }

            // Generate unique filename
            String originalName = file.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }
            String storedFileName = UUID.randomUUID() + extension;

            // Determine content type
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
            boolean isImage = contentType.startsWith("image/");

            // Read file bytes
            byte[] fileBytes = file.getBytes();

            // Persist to Supabase Storage; get a permanent public URL.
            String fileUrl = storageService.upload(storedFileName, fileBytes, contentType);

            // Create media file entity (stores URL only, not the bytes/base64)
            MediaFile mediaFile = MediaFile.builder()
                    .fileName(storedFileName)
                    .originalName(originalName)
                    .fileType(extension)
                    .contentType(contentType)
                    .fileSize((long) fileBytes.length)
                    .fileUrl(fileUrl)
                    .thumbnailUrl(isImage ? fileUrl : null)
                    .uploaderId(uploaderId)
                    .uploaderRole(uploaderRole)
                    .isImage(isImage)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            MediaFile saved = mediaFileRepository.save(mediaFile);
            savedFiles.add(saved);
            log.info("Uploaded file: {} -> {}", originalName, fileUrl);
        }

        return savedFiles;
    }

    /**
     * Get all media files for a user, most recent first.
     */
    public List<MediaFile> getMediaForUser(UUID userId) {
        return mediaFileRepository.findByUploaderId(userId, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /**
     * Get all media files, most recent first.
     */
    public List<MediaFile> getAllMedia() {
        return mediaFileRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    /**
     * Delete a media file by ID.
     */
    public boolean deleteMedia(UUID id, UUID userId) {
        return mediaFileRepository.findById(id)
                .map(mediaFile -> {
                    // Verify ownership or admin role
                    if (mediaFile.getUploaderId() != null && mediaFile.getUploaderId().equals(userId)) {
                        try {
                            // Delete file from cloud storage
                            storageService.delete(mediaFile.getFileName());
                        } catch (Exception e) {
                            log.warn("Failed to delete storage object {}: {}", mediaFile.getFileName(), e.getMessage());
                        }
                        mediaFileRepository.delete(mediaFile);
                        return true;
                    }
                    return false;
                })
                .orElse(false);
    }

    /**
     * Format file size for display.
     */
    public static String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }
}