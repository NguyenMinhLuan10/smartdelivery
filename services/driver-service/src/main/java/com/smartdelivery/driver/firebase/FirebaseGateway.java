package com.smartdelivery.driver.firebase;

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

    public void upsertDriver(UUID driverId, UUID userId, String name, String phone, String email) {
        Map<String, Object> m = new HashMap<>();
        m.put("driverId", driverId.toString());
        m.put("userId", userId != null ? userId.toString() : null);
        m.put("name", name);
        m.put("phone", phone);
        m.put("email", email);
        m.putIfAbsent("online", false);
        m.put("updatedAt", Instant.now().toString());
        root.child("drivers").child(driverId.toString()).updateChildrenAsync(m);
    }

    public void setPresence(UUID driverId, boolean online, Double lat, Double lng, Integer intervalSec) {
        Map<String, Object> m = new HashMap<>();
        m.put("online", online);
        if (lat != null) m.put("lastLat", lat);
        if (lng != null) m.put("lastLng", lng);
        if (intervalSec != null) m.put("intervalSec", intervalSec);
        m.put("lastSeenAt", Instant.now().toString());
        root.child("drivers").child(driverId.toString()).updateChildrenAsync(m);
    }

    public void updateLocation(UUID driverId, double lat, double lng) {
        Map<String, Object> m = new HashMap<>();
        m.put("lastLat", lat);
        m.put("lastLng", lng);
        m.put("lastSeenAt", Instant.now().toString());
        root.child("drivers").child(driverId.toString()).updateChildrenAsync(m);
    }
}
