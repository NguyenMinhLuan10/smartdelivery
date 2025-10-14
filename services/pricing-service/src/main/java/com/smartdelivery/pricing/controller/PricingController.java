package com.smartdelivery.pricing.controller;

import com.smartdelivery.pricing.dto.PriceResponse;
import com.smartdelivery.pricing.dto.QuoteRequest;
import com.smartdelivery.pricing.dto.ServiceTypeDto;
import com.smartdelivery.pricing.model.ServiceType;
import com.smartdelivery.pricing.repository.ServiceTypeRepository;
import com.smartdelivery.pricing.service.PricingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/pricing")
@RequiredArgsConstructor
public class PricingController {

    private final PricingService pricing;
    private final ServiceTypeRepository repo;

    // --- PUBLIC: khách/trang tạo đơn gọi để tính phí ---
    @PostMapping("/quote")
    public PriceResponse quote(@Valid @RequestBody QuoteRequest req) {
        return pricing.quote(req);
    }

    // --- ADMIN: CRUD service types ---
    @GetMapping("/service-types")
    public List<ServiceType> list() {
        return repo.findAll();
    }

    @PostMapping("/service-types")
    @ResponseStatus(HttpStatus.CREATED)
    public ServiceType create(@RequestHeader(value="X-User-Role", required=false) String role,
                              @Valid @RequestBody ServiceTypeDto dto) {
        // kiểm tra role thủ công (gateway đã xác thực)
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new IllegalStateException("Forbidden: ADMIN only");
        }
        return pricing.create(dto);
    }

    @PutMapping("/service-types/{id}")
    public ServiceType update(@RequestHeader(value="X-User-Role", required=false) String role,
                              @PathVariable String id,
                              @Valid @RequestBody ServiceTypeDto dto) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new IllegalStateException("Forbidden: ADMIN only");
        }
        return pricing.update(id, dto);
    }

    @PatchMapping("/service-types/{id}/toggle")
    public Map<String,Object> toggle(@RequestHeader(value="X-User-Role", required=false) String role,
                                     @PathVariable String id) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new IllegalStateException("Forbidden: ADMIN only");
        }
        ServiceType st = repo.findById(id).orElseThrow();
        st.setActive(!Boolean.TRUE.equals(st.getActive()));
        repo.save(st);
        return Map.of("id", id, "active", st.getActive());
    }
}
