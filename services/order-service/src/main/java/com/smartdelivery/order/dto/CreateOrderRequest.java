// dto/CreateOrderRequest.java
package com.smartdelivery.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateOrderRequest {

    @Data public static class Party {
        @NotBlank private String name;
        @NotBlank private String phone;
    }

    @Data public static class Points {
        @NotBlank private String pickupAddress;
        @NotBlank private String dropoffAddress;
    }

    @NotBlank private String serviceTypeCode; // STANDARD/SAME_DAY
    @Valid @NotNull private Party customer;
    @Valid @NotNull private Points points;

    @NotNull private BigDecimal weightKg;
    private BigDecimal valueAmount;
    private Boolean isCOD = false;
    private BigDecimal codAmount;
}
