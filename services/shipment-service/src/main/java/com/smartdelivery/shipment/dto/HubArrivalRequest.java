// dto/HubArrivalRequest.java
package com.smartdelivery.shipment.dto;
import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.NotNull;
import lombok.Data; import java.util.UUID;
@Data
public class HubArrivalRequest {
    private UUID legId;
    @NotNull private UUID orderId;
    @NotBlank private String toHubCode;
    private String note;
}
