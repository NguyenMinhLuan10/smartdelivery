package com.smartdelivery.shipment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class HubScanRequest {

    @NotNull
    private UUID taskId;

    @NotNull
    private String hubCode;
    
    // để nếu QR muốn gửi kèm tracking thì mình check giống pickup
    private String trackingCode;
}
