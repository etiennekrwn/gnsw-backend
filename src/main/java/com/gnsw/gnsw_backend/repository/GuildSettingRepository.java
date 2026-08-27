package com.gnsw.gnsw_backend.repository;

import com.gnsw.gnsw_backend.entity.GuildSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GuildSettingRepository extends JpaRepository<GuildSetting, Long> {
}