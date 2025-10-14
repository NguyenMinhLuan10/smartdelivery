package com.smartdelivery.identity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank private String username; // email or phone
    @NotBlank private String password;
}
