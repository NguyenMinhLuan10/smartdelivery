// dto/PickupScanRequest.java
package com.smartdelivery.shipment.dto;
import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.NotNull;
import lombok.Data; import java.util.UUID;
@Data
public class PickupScanRequest {
    @NotBlank private String trackingCode;
    @NotNull  private UUID taskId;
}
