// controller/OrderController.java
package com.smartdelivery.order.controller;

import com.smartdelivery.order.dto.*;
import com.smartdelivery.order.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService svc;

    @PostMapping
    public OrderDetailResponse create(Authentication auth,
                                      HttpServletRequest http,
                                      @Valid @RequestBody CreateOrderRequest req){
        String authHeader = http.getHeader("Authorization"); // forward tới Gateway
        return svc.create(auth, authHeader, req);
    }

    @GetMapping("/my")
    public Page<OrderListItem> my(Authentication auth, Pageable pageable){
        return svc.myOrders(UUID.fromString(auth.getName()), pageable);
    }

    @GetMapping("/track")
    public OrderDetailResponse track(@RequestParam("code") String code) {
        return svc.getByTracking(code);
    }

    @PostMapping("/{trackingCode}/confirm")
    public Map<String,Object> confirm(Authentication auth,
                                      @PathVariable("trackingCode") String trackingCode,
                                      @Valid @RequestBody ConfirmRequest req){
        svc.confirm(trackingCode, req, auth);
        return Map.of("ok", true);
    }

    @PostMapping("/{trackingCode}/cancel")
    public Map<String,Object> cancel(Authentication auth,
                                     @PathVariable("trackingCode") String trackingCode){
        svc.cancel(trackingCode, auth);
        return Map.of("cancelled", true);
    }

}
