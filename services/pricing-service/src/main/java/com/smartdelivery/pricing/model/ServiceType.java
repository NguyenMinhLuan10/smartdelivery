package com.smartdelivery.pricing.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "service_types",
        indexes = {
                @Index(name = "idx_service_types_code", columnList = "code", unique = true),
                @Index(name = "idx_service_types_active", columnList = "active")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceType {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true, length = 32)
    private String code; // STANDARD/SAME_DAY/INTERCITY

    @Column(nullable = false, length = 128)
    private String name;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(precision = 16, scale = 2, nullable = false)
    private BigDecimal basePrice = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 16, scale = 2, nullable = false)
    private BigDecimal perKmPrice = BigDecimal.ZERO;

    @Builder.Default
    @Column(precision = 16, scale = 2, nullable = false)
    private BigDecimal perKgPrice = BigDecimal.ZERO;

    private Integer volumetricDivisor; // ví dụ 5000

    @Builder.Default
    @Column(length = 8, nullable = false)
    private String currency = "VND";

    private Integer slaHours;
    private LocalTime cutoffTime;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
