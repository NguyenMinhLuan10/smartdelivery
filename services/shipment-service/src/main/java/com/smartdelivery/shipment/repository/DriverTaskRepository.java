// repository/DriverTaskRepository.java
package com.smartdelivery.shipment.repository;
import com.smartdelivery.shipment.model.DriverTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.UUID;
public interface DriverTaskRepository extends JpaRepository<DriverTask, UUID> {
    List<DriverTask> findByDriverIdOrderByAssignedAtDesc(UUID driverId);
}
