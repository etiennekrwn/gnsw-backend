package com.gns.gns_backend.service;

import com.gns.gns_backend.entity.MediaFile;
import com.gns.gns_backend.repository.MediaFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaService {

    private final MediaFileRepository mediaFileRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.frontend.member-portal-url:http://localhost:5175}")
    private String memberPortalUrl;

    /**
     * Upload one or more files to local filesystem and persist metadata to DB.
     */
    public List<MediaFile> uploadFiles(List<MultipartFile> files, UUID uploaderId, String uploaderRole) throws IOException {
        List<MediaFile> savedFiles = new java.util.ArrayList<>();

        // Ensure upload directory exists
        Path uploadPath = Paths.get(uploadDir);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

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
            String storedFileName = UUID.randomUUID().toString() + extension;

            // Determine content type
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

            // Read file bytes
            byte[] fileBytes = file.getBytes();

            // Save to filesystem
            Path filePath = uploadPath.resolve(storedFileName);
            Files.write(filePath, fileBytes);

            // Determine if image
            boolean isImage = contentType != null && contentType.startsWith("image/");
            String thumbnailUrl = null;

            // For images, generate a thumbnail URL (same path for now)
            String fileUrl = "/uploads/" + storedFileName;
            if (isImage) {
                thumbnailUrl = fileUrl;
            }

            // Create media file entity
            MediaFile mediaFile = MediaFile.builder()
                    .fileName(storedFileName)
                    .originalName(originalName)
                    .fileType(extension)
                    .contentType(contentType)
                    .fileSize((long) fileBytes.length)
                    .fileUrl(fileUrl)
                    .thumbnailUrl(thumbnailUrl)
                    .uploaderId(uploaderId)
                    .uploaderRole(uploaderRole)
                    .isImage(isImage)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            MediaFile saved = mediaFileRepository.save(mediaFile);
            savedFiles.add(saved);
            log.info("Uploaded file: {} -> {}", originalName, storedFileName);
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
                            // Delete file from filesystem
                            Path filePath = Paths.get(uploadDir, mediaFile.getFileName());
                            if (Files.exists(filePath)) {
                                Files.deleteIfExists(filePath);
                            }
                        } catch (IOException e) {
                            log.warn("Failed to delete file {}: {}", mediaFile.getFileName(), e.getMessage());
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