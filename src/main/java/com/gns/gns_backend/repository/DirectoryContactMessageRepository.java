package com.gns.gns_backend.repository;

import com.gns.gns_backend.entity.DirectoryContactMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface DirectoryContactMessageRepository extends JpaRepository<DirectoryContactMessage, UUID> {
}