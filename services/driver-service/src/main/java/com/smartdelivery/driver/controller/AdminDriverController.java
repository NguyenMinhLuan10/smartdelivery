package com.smartdelivery.driver.controller;

import com.smartdelivery.driver.dto.*;
import com.smartdelivery.driver.service.DriverService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController @RequestMapping
@RequiredArgsConstructor
public class AdminDriverController {
    private final DriverService svc;

    @GetMapping("/drivers")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public List<DriverSummary> list(){ return svc.list(); }

    @GetMapping("/drivers/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public DriverSummary get(@PathVariable UUID id){ return svc.get(id); }

    @PatchMapping("/drivers/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','DISPATCHER')")
    public DriverSummary status(@PathVariable UUID id, @RequestBody StatusReq req){
        return svc.status(id, req.getOnlineStatus(), req.getActive());
    }

    @PostMapping("/internal/drivers/sync")
    public DriverSyncResponse sync(@Valid @RequestBody DriverSyncRequest req){
        var r = svc.upsert(req);
        return new DriverSyncResponse(r.driverId(), r.created(), r.synced());
    }
}
