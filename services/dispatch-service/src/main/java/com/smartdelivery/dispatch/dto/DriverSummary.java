// dto/DriverSummary.java
package com.smartdelivery.dispatch.dto;
import lombok.Builder; import lombok.Data; import java.time.OffsetDateTime; import java.util.UUID;
@Data @Builder
public class DriverSummary {
    private UUID id;
    private UUID userId;
    private String homeHubCode;
    private Boolean active;
    private String onlineStatus;
    private Double lastLat;
    private Double lastLng;
    private OffsetDateTime lastSeenAt;
}
