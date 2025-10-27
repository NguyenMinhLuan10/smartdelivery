package com.smartdelivery.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ConfirmRequest {
    @NotBlank private String method; // COD / VNPAY / STRIPE
}
