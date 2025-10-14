// repository/PodRepository.java
package com.smartdelivery.shipment.repository;
import com.smartdelivery.shipment.model.Pod;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
public interface PodRepository extends JpaRepository<Pod, UUID> { }
