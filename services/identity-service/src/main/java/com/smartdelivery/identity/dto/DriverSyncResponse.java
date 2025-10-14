// identity-service: client/dto/DriverSyncResponse.java
package com.smartdelivery.identity.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class DriverSyncResponse {
    private UUID driverId;
    private boolean created;
    private boolean synced;
}
