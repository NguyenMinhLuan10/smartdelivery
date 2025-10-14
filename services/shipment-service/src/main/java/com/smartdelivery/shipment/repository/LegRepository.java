// repository/LegRepository.java
package com.smartdelivery.shipment.repository;
import com.smartdelivery.shipment.model.Leg;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.UUID;
public interface LegRepository extends JpaRepository<Leg, UUID> {
    List<Leg> findByOrderIdOrderBySeqNoAsc(UUID orderId);
}
