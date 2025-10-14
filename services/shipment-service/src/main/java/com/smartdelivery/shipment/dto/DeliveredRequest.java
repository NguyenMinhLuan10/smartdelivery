// dto/DeliveredRequest.java
package com.smartdelivery.shipment.dto;
import jakarta.validation.constraints.NotNull; import lombok.Data;
import java.math.BigDecimal; import java.util.UUID;
@Data
public class DeliveredRequest {
    @NotNull private UUID taskId;
    private String method;      // OTP/QR/PHOTO
    private String otpCode;
    private String photoUrl;
    private String signedName;
    private String signedPhone;
    private BigDecimal codAmount;
}
