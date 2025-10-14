// controller/AssignController.java
package com.smartdelivery.shipment.controller;

import com.smartdelivery.shipment.dto.AssignRequest;
import com.smartdelivery.shipment.dto.TaskSummaryResponse;
import com.smartdelivery.shipment.service.AssignService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/ship")
@RequiredArgsConstructor
public class AssignController {
    private final AssignService svc;

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public TaskSummaryResponse assign(@Valid @RequestBody AssignRequest req, HttpServletRequest http){
        return svc.assign(req, http);
    }
}
