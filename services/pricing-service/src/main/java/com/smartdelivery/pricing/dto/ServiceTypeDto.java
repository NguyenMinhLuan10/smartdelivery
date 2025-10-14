package com.smartdelivery.pricing.dto;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceTypeDto {
    @NotBlank @Size(max=32)
    private String code;

    @NotBlank @Size(max=128)
    private String name;

    private boolean active = true;

    @NotNull @DecimalMin("0.0")
    private BigDecimal basePrice;

    @NotNull @DecimalMin("0.0")
    private BigDecimal perKmPrice;

    @NotNull @DecimalMin("0.0")
    private BigDecimal perKgPrice;

    private Integer volumetricDivisor; // nullable
    @NotBlank
    private String currency = "VND";
    private Integer slaHours;
    private LocalTime cutoffTime;
}
