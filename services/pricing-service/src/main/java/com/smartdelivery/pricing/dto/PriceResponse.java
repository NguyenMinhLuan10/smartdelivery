package com.smartdelivery.pricing.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PriceResponse {
    private String serviceTypeCode;
    private String currency;
    private BigDecimal priceAmount;       // tổng phí ship
    private BigDecimal basePrice;
    private BigDecimal distanceFee;
    private BigDecimal weightFee;
    private double chargeableWeightKg;
    private String breakdown; // mô tả ngắn gọn
}
