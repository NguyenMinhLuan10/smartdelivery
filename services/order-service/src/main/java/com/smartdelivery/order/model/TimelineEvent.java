// model/TimelineEvent.java
package com.smartdelivery.order.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="timeline_events",
        indexes = @Index(name="idx_events_entity_ts", columnList="entity_type,entity_id,ts"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TimelineEvent {
    @Id @GeneratedValue @UuidGenerator
    private UUID id;

    private String entityType; // ORDER
    private UUID entityId;

    private String eventType;  // ORDER_STATUS_CHANGED
    private String fromStatus;
    private String toStatus;
    private String reason;

    private OffsetDateTime ts;
    private String meta;

    @PrePersist void preInsert(){ ts = OffsetDateTime.now(); }
}
