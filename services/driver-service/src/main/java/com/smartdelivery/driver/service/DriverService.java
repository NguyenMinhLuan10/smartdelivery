package com.smartdelivery.driver.service;

import com.smartdelivery.driver.dto.DriverSummary;
import com.smartdelivery.driver.dto.DriverSyncRequest;
import com.smartdelivery.driver.model.Driver;
import com.smartdelivery.driver.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class DriverService {
    private final DriverRepository repo;

    public List<DriverSummary> list(){
        return repo.findAll().stream().map(this::toSummary).toList();
    }

    public DriverSummary get(UUID id){
        return toSummary(repo.findById(id).orElseThrow());
    }

    @Transactional
    public DriverSummary status(UUID id, String onlineStatus, Boolean active){
        var d = repo.findById(id).orElseThrow();
        if (onlineStatus!=null) d.setOnlineStatus(onlineStatus);
        if (active!=null) d.setActive(active);
        return toSummary(repo.save(d));
    }

    @Transactional
    public DriverSyncResponse upsert(DriverSyncRequest req){
        var opt = repo.findByUserId(req.getUserId());
        if (opt.isPresent()){
            var d = opt.get();
            if (d.getActive()==null) d.setActive(true);
            if (d.getOnlineStatus()==null) d.setOnlineStatus("OFFLINE");
            return new DriverSyncResponse(d.getId(), false, true);
        } else {
            var d = Driver.builder()
                    .userId(req.getUserId())
                    .active(true).onlineStatus("OFFLINE").build();
            repo.save(d);
            return new DriverSyncResponse(d.getId(), true, true);
        }
    }

    private DriverSummary toSummary(Driver d){
        return DriverSummary.builder()
                .id(d.getId()).userId(d.getUserId()).homeHubCode(d.getHomeHubCode())
                .active(d.getActive()).onlineStatus(d.getOnlineStatus())
                .lastLat(d.getLastLat()).lastLng(d.getLastLng()).lastSeenAt(d.getLastSeenAt())
                .build();
    }

    public record DriverSyncResponse(UUID driverId, boolean created, boolean synced){}
}
