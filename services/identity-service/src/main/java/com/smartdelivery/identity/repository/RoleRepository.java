package com.smartdelivery.identity.repository;

import com.smartdelivery.identity.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {
    boolean existsByCode(String code);
    Optional<Role> findByCode(String code);
}
