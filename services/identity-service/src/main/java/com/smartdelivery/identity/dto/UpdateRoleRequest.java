// dto/UpdateRoleRequest.java
package com.smartdelivery.identity.dto;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateRoleRequest {
    @NotBlank
    private String role;  // "ADMIN" | "DISPATCHER" | "DRIVER" | "CUSTOMER"
}
