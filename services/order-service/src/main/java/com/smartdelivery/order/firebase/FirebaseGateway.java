package com.smartdelivery.order.firebase;

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

    /**
     * Đẩy / cập nhật 1 đơn lên Realtime DB
     */
    public void upsertOrder(UUID orderId,
                            String tracking,
                            String status,
                            Map<String,Object> geo,
                            UUID driverId) {
        Map<String,Object> m = new HashMap<>();
        m.put("orderId", orderId.toString());
        if (tracking != null) m.put("trackingCode", tracking);
        if (status != null)   m.put("status", status);
        if (geo != null)      m.putAll(geo);
        if (driverId != null) m.put("assignedDriverId", driverId.toString());
        m.put("updatedAt", Instant.now().toString());

        // /orders/{orderId}
        root.child("orders").child(orderId.toString()).updateChildrenAsync(m);

        // thêm lookup theo code
        if (tracking != null) {
            root.child("ordersByCode").child(tracking).setValueAsync(orderId.toString());
        }
    }
}
