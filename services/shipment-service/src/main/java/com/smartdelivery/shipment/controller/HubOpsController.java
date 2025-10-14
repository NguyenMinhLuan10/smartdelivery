// controller/HubOpsController.java
package com.smartdelivery.shipment.controller;

import com.smartdelivery.shipment.dto.HubArrivalRequest;
import com.smartdelivery.shipment.dto.HubOutboundRequest;
import com.smartdelivery.shipment.service.HubFlowService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController @RequestMapping("/hub")
@RequiredArgsConstructor
public class HubOpsController {
    private final HubFlowService hubFlow;

    @PostMapping("/scan/outbound") @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public Map<String,Object> outbound(@Valid @RequestBody HubOutboundRequest req, HttpServletRequest http){
        hubFlow.outbound(req, http); return Map.of("updated", true);
    }

    @PostMapping("/scan/arrival") @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public Map<String,Object> arrival(@Valid @RequestBody HubArrivalRequest req, HttpServletRequest http){
        hubFlow.arrival(req, http); return Map.of("updated", true);
    }
}
