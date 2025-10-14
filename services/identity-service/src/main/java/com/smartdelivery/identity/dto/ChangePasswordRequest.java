package com.smartdelivery.identity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank
    private String oldPwd;

    @NotBlank
    private String newPwd;
}
