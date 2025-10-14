// repository/TimelineEventRepository.java
package com.smartdelivery.order.repository;

import com.smartdelivery.order.model.TimelineEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TimelineEventRepository extends JpaRepository<TimelineEvent, UUID> {
    List<TimelineEvent> findByEntityTypeAndEntityIdOrderByTsAsc(String entityType, UUID entityId);
}
