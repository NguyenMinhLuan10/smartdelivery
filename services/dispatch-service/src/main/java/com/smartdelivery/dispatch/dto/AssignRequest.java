// dto/AssignRequest.java
package com.smartdelivery.dispatch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data public class AssignRequest {
    @NotBlank private String trackingCode;
    @NotBlank private String type;
    @NotNull private UUID driverId;
}
