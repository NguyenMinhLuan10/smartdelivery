package com.smartdelivery.shipment.firebase;

import com.google.firebase.database.DatabaseReference;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class FirebaseGateway {

    private final DatabaseReference root;

    public void upsertOrderAssign(UUID orderId, String tracking, UUID driverId) {
        Map<String,Object> m = new HashMap<>();
        m.put("trackingCode", tracking);
        m.put("driverId", driverId != null ? driverId.toString() : null);
        m.put("updatedAt", Instant.now().toString());
        root.child("orders").child(orderId.toString()).updateChildrenAsync(m);
    }

    public void upsertTask(UUID taskId, UUID orderId, String tracking, String status, UUID driverId) {
        Map<String,Object> m = new HashMap<>();
        m.put("taskId", taskId.toString());
        m.put("orderId", orderId.toString());
        m.put("trackingCode", tracking);
        m.put("status", status);
        if (driverId != null) m.put("driverId", driverId.toString());
        m.put("updatedAt", Instant.now().toString());
        root.child("tasks").child(taskId.toString()).updateChildrenAsync(m);
    }

    /**
     * Đẩy task cho tài xế, thêm được hubAddress/hubLat/hubLng
     */
    public void pushTaskForDriver(
            UUID driverId,
            UUID taskId,
            UUID orderId,
            String tracking,
            String displayAddress,
            Double displayLat,
            Double displayLng,
            String legType,
            String pickupAddress,
            Double pickupLat,
            Double pickupLng,
            String dropoffAddress,
            Double dropoffLat,
            Double dropoffLng,
            String receiverName,
            String receiverPhone,
            String hubAddress,
            Double hubLat,
            Double hubLng
    ) {
        Map<String,Object> m = new HashMap<>();
        m.put("taskId", taskId.toString());
        m.put("orderId", orderId.toString());
        m.put("trackingCode", tracking);
        m.put("status", "ASSIGNED");

        // cái app sẽ show ngay
        m.put("address", displayAddress);
        if (displayLat != null) m.put("lat", displayLat);
        if (displayLng != null) m.put("lng", displayLng);

        // pickup
        if (pickupAddress != null) m.put("pickupAddress", pickupAddress);
        if (pickupLat != null) m.put("pickupLat", pickupLat);
        if (pickupLng != null) m.put("pickupLng", pickupLng);

        // dropoff
        if (dropoffAddress != null) m.put("dropoffAddress", dropoffAddress);
        if (dropoffLat != null) m.put("dropoffLat", dropoffLat);
        if (dropoffLng != null) m.put("dropoffLng", dropoffLng);

        if (receiverName != null) m.put("receiverName", receiverName);
        if (receiverPhone != null) m.put("receiverPhone", receiverPhone);

        if (legType != null) {
            m.put("type", legType.toUpperCase());
        }

        // 👇 thêm hub
        if (hubAddress != null) m.put("hubAddress", hubAddress);
        if (hubLat != null)    m.put("hubLat", hubLat);
        if (hubLng != null)    m.put("hubLng", hubLng);

        m.put("updatedAt", Instant.now().toString());

        root.child("driver-tasks")
                .child(driverId.toString())
                .child(taskId.toString())
                .setValueAsync(m);
    }

    public void updateDriverTaskStatus(UUID driverId, UUID taskId, String status) {
        if (driverId == null || taskId == null) return;
        Map<String,Object> m = new HashMap<>();
        m.put("status", status);
        m.put("updatedAt", Instant.now().toString());
        root.child("driver-tasks")
                .child(driverId.toString())
                .child(taskId.toString())
                .updateChildrenAsync(m);
    }

    public void deleteDriverTask(UUID driverId, UUID taskId) {
        if (driverId == null || taskId == null) return;
        root.child("driver-tasks")
                .child(driverId.toString())
                .child(taskId.toString())
                .removeValueAsync();
    }
}
