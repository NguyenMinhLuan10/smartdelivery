// controller/DispatcherController.java
package com.smartdelivery.dispatch.controller;

import com.smartdelivery.dispatch.client.GwClient;
import com.smartdelivery.dispatch.dto.AssignRequest;
import com.smartdelivery.dispatch.dto.TaskSummaryResponse;
import com.smartdelivery.dispatch.dto.DriverSummary;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController @RequestMapping("/dispatch") @RequiredArgsConstructor
public class DispatcherController {
    private final GwClient gw;

    @PostMapping("/assign")
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public TaskSummaryResponse assign(@Valid @RequestBody AssignRequest req, HttpServletRequest http){
        String auth = http.getHeader("Authorization");
        return gw.post("/ship/assign", req, TaskSummaryResponse.class, auth);
    }

    @GetMapping("/drivers")
    @PreAuthorize("hasAnyRole('DISPATCHER','ADMIN')")
    public List<DriverSummary> listDrivers(HttpServletRequest http){
        String auth = http.getHeader("Authorization");
        var arr = gw.get("/drivers", DriverSummary[].class, auth);
        return Arrays.asList(arr);
    }
}
