package com.gns.gns_backend.repository;

import com.gns.gns_backend.entity.AdminUser;
import com.gns.gns_backend.enums.AdminRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminUserRepository extends JpaRepository<AdminUser, UUID> {

    Optional<AdminUser> findByEmail(String email);

    Optional<AdminUser> findByInviteTokenHash(String tokenHash);

    List<AdminUser> findAllByOrderByCreatedAtDesc();

    long countByRole(AdminRole role);
}