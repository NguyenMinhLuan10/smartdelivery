package com.smartdelivery.identity.repository;

import com.smartdelivery.identity.model.AuditAuth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditAuthRepository extends JpaRepository<AuditAuth, UUID> {}
