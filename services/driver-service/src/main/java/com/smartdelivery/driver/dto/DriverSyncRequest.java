package com.smartdelivery.driver.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class DriverSyncRequest {
    @NotNull private UUID userId;
    private String fullName;
    private String phone;
    private String email;
}
