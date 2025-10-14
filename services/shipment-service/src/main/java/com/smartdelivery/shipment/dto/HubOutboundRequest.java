// dto/HubOutboundRequest.java
package com.smartdelivery.shipment.dto;
import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.NotNull;
import lombok.Data; import java.util.UUID;
@Data
public class HubOutboundRequest {
    private UUID legId;
    private UUID orderId;
    @NotBlank private String fromHubCode;
    @NotBlank private String toHubCode;
    private String note;
}
