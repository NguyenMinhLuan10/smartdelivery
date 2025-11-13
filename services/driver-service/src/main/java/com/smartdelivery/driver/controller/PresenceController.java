package com.smartdelivery.driver.controller;

import com.smartdelivery.driver.firebase.FirebaseGateway;
import com.smartdelivery.driver.model.Driver;
import com.smartdelivery.driver.repository.DriverRepository;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/driver")
@RequiredArgsConstructor
public class PresenceController {

    private final DriverRepository repo;
    private final FirebaseGateway firebase;

    private Driver requireMe(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return repo.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Driver not found for user"));
    }

    /** Driver bật/tắt online & quy định tần suất client ping (giây) */
    @PostMapping("/presence")
    @PreAuthorize("hasRole('DRIVER')")
    public Map<String,Object> presence(@RequestBody PresenceReq req, Authentication auth) {
        Driver d = requireMe(auth);
        if (req.getOnline() != null) {
            d.setOnlineStatus(Boolean.TRUE.equals(req.getOnline()) ? "ONLINE" : "OFFLINE");
        }
        if (req.getLat() != null) d.setLastLat(req.getLat());
        if (req.getLng() != null) d.setLastLng(req.getLng());
        d.setLastSeenAt(OffsetDateTime.now());
        repo.save(d);

        firebase.setPresence(d.getId(),
                "ONLINE".equalsIgnoreCase(d.getOnlineStatus()) || "BUSY".equalsIgnoreCase(d.getOnlineStatus()),
                req.getLat(), req.getLng(), req.getIntervalSec());

        return Map.of(
                "onlineStatus", d.getOnlineStatus(),
                "intervalSec", 5
        );
    }

    /** Client gọi mỗi N giây khi online để cập nhật toạ độ */
    @PostMapping("/location/ping")
    @PreAuthorize("hasRole('DRIVER')")
    public Map<String,Object> ping(@RequestBody PingReq req, Authentication auth) {
        Driver d = requireMe(auth);
        if (!"ONLINE".equalsIgnoreCase(d.getOnlineStatus()) && !"BUSY".equalsIgnoreCase(d.getOnlineStatus()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Driver is offline");

        d.setLastLat(req.getLat());
        d.setLastLng(req.getLng());
        d.setLastSeenAt(OffsetDateTime.now());
        repo.save(d);

        firebase.updateLocation(d.getId(), req.getLat(), req.getLng());
        return Map.of("ok", true, "ts", d.getLastSeenAt().toString());
    }

    @Data
    public static class PresenceReq {
        private Boolean online;    // true/false
        private Double lat;        // optional (khi bật online lần đầu)
        private Double lng;        // optional
        private Integer intervalSec; // gợi ý client ping mỗi N giây (vd 10–15s)
    }

    @Data
    public static class PingReq {
        @NotNull private Double lat;
        @NotNull private Double lng;
        private Double speed;
        private Double heading;
    }
}
