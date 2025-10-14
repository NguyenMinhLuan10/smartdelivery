// dto/OutForDeliveryRequest.java
package com.smartdelivery.shipment.dto;
import jakarta.validation.constraints.NotNull; import lombok.Data; import java.util.UUID;
@Data public class OutForDeliveryRequest { @NotNull private UUID taskId; }
