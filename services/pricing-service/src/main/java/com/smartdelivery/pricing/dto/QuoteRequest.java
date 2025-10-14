package com.smartdelivery.pricing.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuoteRequest {
    @NotBlank
    private String serviceTypeCode;

    @PositiveOrZero
    private double distanceKm;     // từ Maps/Distance Matrix

    @PositiveOrZero
    private double weightKg;

    // kích thước (cm) – có thể = 0
    @PositiveOrZero private double dimL;
    @PositiveOrZero private double dimW;
    @PositiveOrZero private double dimH;

    private boolean isCod;
    @PositiveOrZero
    private double codAmount; // nếu isCod
}
