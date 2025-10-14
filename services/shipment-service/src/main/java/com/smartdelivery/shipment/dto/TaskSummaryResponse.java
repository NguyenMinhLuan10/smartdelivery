// dto/TaskSummaryResponse.java
package com.smartdelivery.shipment.dto;
import lombok.Builder; import lombok.Data;
import java.time.OffsetDateTime; import java.util.UUID;
@Data @Builder
public class TaskSummaryResponse {
    private UUID taskId;
    private UUID legId;
    private String legType;
    private String roleHint;
    private String trackingCode;
    private String status;
    private OffsetDateTime assignedAt;
}
