// repository/HubRepository.java
package com.smartdelivery.shipment.repository;
import com.smartdelivery.shipment.model.Hub;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional; import java.util.UUID;
public interface HubRepository extends JpaRepository<Hub, UUID> {
    Optional<Hub> findByCode(String code);
}
