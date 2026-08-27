package com.gnsw.gnsw_backend.repository;

import com.gnsw.gnsw_backend.entity.MemberPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface MemberPreferenceRepository extends JpaRepository<MemberPreference, UUID> {
    Optional<MemberPreference> findByUserId(UUID userId);
}