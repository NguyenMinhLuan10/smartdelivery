package com.smartdelivery.identity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateStatusRequest {
    @NotBlank private String status; // ACTIVE/INACTIVE/BLOCKED
}
