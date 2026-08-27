package com.gns.gns_backend.repository;

import com.gns.gns_backend.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;
import java.util.List;
import java.util.UUID;

public interface MediaFileRepository extends JpaRepository<MediaFile, UUID> {
    List<MediaFile> findByUploaderId(UUID uploaderId, Sort sort);
    List<MediaFile> findByIsImageTrue(Sort sort);
    void deleteByUploaderId(UUID uploaderId);
}