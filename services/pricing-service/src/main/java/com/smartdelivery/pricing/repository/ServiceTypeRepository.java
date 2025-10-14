package com.smartdelivery.pricing.repository;

import com.smartdelivery.pricing.model.ServiceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceTypeRepository extends JpaRepository<ServiceType, String> {
    Optional<ServiceType> findByCodeAndActiveTrue(String code);
    boolean existsByCode(String code);
}
