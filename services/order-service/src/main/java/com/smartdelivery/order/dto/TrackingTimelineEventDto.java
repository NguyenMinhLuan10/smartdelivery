// dto/TrackingTimelineEventDto.java
package com.smartdelivery.order.dto;

import lombok.Builder; import lombok.Data;
import java.time.OffsetDateTime;

@Data @Builder
public class TrackingTimelineEventDto {
    private String type;
    private String fromStatus;
    private String toStatus;
    private String reason;
    private OffsetDateTime ts;
}
