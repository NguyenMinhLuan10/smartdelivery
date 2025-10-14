package com.smartdelivery.shipment.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity @Table(name="legs",
        uniqueConstraints=@UniqueConstraint(name="uq_legs_order_seq", columnNames={"orderId","seqNo"}),
        indexes=@Index(name="idx_legs_status", columnList="status"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Leg {
    @Id @GeneratedValue @UuidGenerator
    private UUID id;

    private UUID orderId;
    private Integer seqNo;

    private String legType;   // PICKUP/LINEHAUL/DELIVERY
    private String status;    // PLANNED/ASSIGNED/IN_TRANSIT/COMPLETED/FAILED

    private UUID fromHubId;
    private UUID toHubId;

    private OffsetDateTime plannedStart;
    private OffsetDateTime plannedEnd;
    private OffsetDateTime actualStart;
    private OffsetDateTime actualEnd;
}
