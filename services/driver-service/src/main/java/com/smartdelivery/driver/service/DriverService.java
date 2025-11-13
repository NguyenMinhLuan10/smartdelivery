package com.smartdelivery.driver.service;

import com.smartdelivery.driver.dto.DriverSummary;
import com.smartdelivery.driver.dto.DriverSyncRequest;
import com.smartdelivery.driver.firebase.FirebaseGateway;
import com.smartdelivery.driver.model.Driver;
import com.smartdelivery.driver.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class DriverService {
    private final DriverRepository repo;
    private final FirebaseGateway firebase;

    public List<DriverSummary> list(){
        return repo.findAll().stream().map(this::toSummary).toList();
    }

    public DriverSummary get(UUID id){
        return toSummary(repo.findById(id).orElseThrow());
    }

    @Transactional
    public DriverSummary status(UUID id, String onlineStatus, Boolean active) {
        Driver d = repo.findById(id).orElseThrow();
        if (onlineStatus != null) d.setOnlineStatus(onlineStatus);
        if (active != null) d.setActive(active);
        d.setLastSeenAt(OffsetDateTime.now());
        repo.save(d);

        // cập nhật “online” sang Firebase theo ONLINE/OFFLINE/BUSY
        boolean online = "ONLINE".equalsIgnoreCase(d.getOnlineStatus()) || "BUSY".equalsIgnoreCase(d.getOnlineStatus());
        firebase.setPresence(d.getId(), online, d.getLastLat(), d.getLastLng(), null);

        return toSummary(d);
    }


    /** upsert từ identity-service */
    @Transactional
    public UpsertResult upsert(DriverSyncRequest req) {
        Driver d = repo.findByUserId(req.getUserId()).orElse(null);
        boolean created = false;

        if (d == null) {
            d = Driver.builder()
                    .userId(req.getUserId())
                    .active(true)
                    .onlineStatus("OFFLINE")
                    .createdAt(OffsetDateTime.now())
                    .build();
            created = true;
        }
        // bạn có thể lưu thêm metadata tên/phone/email ở bảng riêng; ở đây ta chỉ sync Firebase
        repo.saveAndFlush(d);

        // ghi Firebase hồ sơ tài xế (đủ tên/phone/email như user)
        firebase.upsertDriver(d.getId(), req.getUserId(), req.getFullName(), req.getPhone(), req.getEmail());

        return new UpsertResult(d.getId(), created, true);
    }

    public record UpsertResult(UUID driverId, boolean created, boolean synced) {}

    private DriverSummary toSummary(Driver d) {
        return DriverSummary.builder()
                .id(d.getId())
                .userId(d.getUserId())
                .homeHubCode(d.getHomeHubCode())
                .active(d.getActive())
                .onlineStatus(d.getOnlineStatus())
                .lastLat(d.getLastLat())
                .lastLng(d.getLastLng())
                .lastSeenAt(d.getLastSeenAt())
                .build();
    }

}
