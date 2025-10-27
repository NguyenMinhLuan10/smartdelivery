package com.smartdelivery.order.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name="attachments",
        indexes = @Index(name="idx_attach_entity_ts", columnList="entity_type,entity_id,created_at"))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Attachment {
    @Id @GeneratedValue @UuidGenerator
    private UUID id;

    private String entityType; // ORDER
    private UUID entityId;

    private String url;        // images/qrcodes/<tracking>.png
    private String mime;       // image/png

    private OffsetDateTime createdAt;

    @PrePersist void preInsert(){ createdAt = OffsetDateTime.now(); }
}
