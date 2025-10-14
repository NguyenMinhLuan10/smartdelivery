package com.smartdelivery.identity.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class SignupCustomerRequest {
    @NotBlank @Size(max=128) private String name;
    @Email @NotBlank private String email;
    @Pattern(regexp="^[0-9+]{8,15}$") @NotBlank private String phone;
    @NotBlank @Size(min=6,max=100) private String password;
}
