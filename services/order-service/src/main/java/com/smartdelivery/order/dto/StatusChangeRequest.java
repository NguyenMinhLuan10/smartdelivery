// dto/StatusChangeRequest.java
package com.smartdelivery.order.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.UUID;

@Data
public class StatusChangeRequest {
    @NotBlank private String toStatus;
    private String reason;
    private UUID hubId;
    private UUID actorUserId;
}
