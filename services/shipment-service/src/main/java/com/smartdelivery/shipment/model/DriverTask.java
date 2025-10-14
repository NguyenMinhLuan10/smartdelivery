package com.smartdelivery.shipment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="driver_tasks",
        indexes = {
                @Index(name="idx_tasks_driver_status", columnList="driverId,status"),
                @Index(name="idx_tasks_leg", columnList="legId"),
                @Index(name="idx_tasks_assigned_at", columnList="assignedAt")
        })
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DriverTask {
    @Id @GeneratedValue @UuidGenerator
    private UUID id;

    private UUID legId;
    private UUID orderId;
    private String trackingCode;

    private UUID driverId;
    private UUID assignedBy;
    private OffsetDateTime assignedAt;

    private String status;              // ASSIGNED/ACCEPTED/STARTED/DELIVERED/FAILED/CANCELLED
    private OffsetDateTime startTime;
    private OffsetDateTime deliveredTime;
    private OffsetDateTime failedTime;
    private String failReason;

    private BigDecimal codCollected;
}
