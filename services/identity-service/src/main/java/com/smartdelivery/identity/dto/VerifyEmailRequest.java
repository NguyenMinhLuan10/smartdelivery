package com.smartdelivery.identity.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyEmailRequest {
    @Email @NotBlank private String email;
    @NotBlank private String code;
}
