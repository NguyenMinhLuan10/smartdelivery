// dto/AssignRequest.java
package com.smartdelivery.shipment.dto;
import jakarta.validation.constraints.NotBlank; import jakarta.validation.constraints.NotNull;
import lombok.Data; import java.util.UUID;
@Data
public class AssignRequest {
    @NotBlank private String trackingCode;    // SDxxxx
    @NotBlank private String type;            // PICKUP / DELIVERY / AUTO
    @NotNull  private UUID driverId;
}
