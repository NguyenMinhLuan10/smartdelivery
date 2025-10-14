package com.smartdelivery.pricing.service;

import com.smartdelivery.pricing.dto.PriceResponse;
import com.smartdelivery.pricing.dto.QuoteRequest;
import com.smartdelivery.pricing.dto.ServiceTypeDto;
import com.smartdelivery.pricing.model.ServiceType;
import com.smartdelivery.pricing.repository.ServiceTypeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class PricingService {

    private final ServiceTypeRepository repo;

    // CRUD cấu hình service type
    @Transactional
    public ServiceType create(ServiceTypeDto dto){
        if (repo.existsByCode(dto.getCode()))
            throw new IllegalArgumentException("ServiceType code duplicated: " + dto.getCode());
        ServiceType st = ServiceType.builder()
                .code(dto.getCode())
                .name(dto.getName())
                .active(dto.isActive())
                .basePrice(dto.getBasePrice())
                .perKmPrice(dto.getPerKmPrice())
                .perKgPrice(dto.getPerKgPrice())
                .volumetricDivisor(dto.getVolumetricDivisor())
                .currency(dto.getCurrency())
                .slaHours(dto.getSlaHours())
                .cutoffTime(dto.getCutoffTime())
                .build();
        return repo.save(st);
    }

    @Transactional
    public ServiceType update(String id, ServiceTypeDto dto){
        ServiceType st = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("ServiceType not found"));
        st.setName(dto.getName());
        st.setActive(dto.isActive());
        st.setBasePrice(dto.getBasePrice());
        st.setPerKmPrice(dto.getPerKmPrice());
        st.setPerKgPrice(dto.getPerKgPrice());
        st.setVolumetricDivisor(dto.getVolumetricDivisor());
        st.setCurrency(dto.getCurrency());
        st.setSlaHours(dto.getSlaHours());
        st.setCutoffTime(dto.getCutoffTime());
        return repo.save(st);
    }

    // Quote: tính phí ship cho 1 request
    @Transactional(readOnly = true)
    public PriceResponse quote(QuoteRequest req){
        ServiceType st = repo.findByCodeAndActiveTrue(req.getServiceTypeCode())
                .orElseThrow(() -> new IllegalArgumentException("Service type inactive or not found"));

        // Trọng lượng quy đổi
        double volumetric = 0.0;
        if (st.getVolumetricDivisor() != null && st.getVolumetricDivisor() > 0
                && req.getDimL() > 0 && req.getDimW() > 0 && req.getDimH() > 0) {
            // cm^3 / divisor => kg
            volumetric = (req.getDimL() * req.getDimW() * req.getDimH()) / st.getVolumetricDivisor();
        }
        double chargeable = Math.max(req.getWeightKg(), volumetric);

        BigDecimal base = st.getBasePrice();
        BigDecimal distFee = st.getPerKmPrice().multiply(BigDecimal.valueOf(req.getDistanceKm()));
        BigDecimal weightFee = st.getPerKgPrice().multiply(BigDecimal.valueOf(chargeable));

        BigDecimal total = base.add(distFee).add(weightFee);
        total = total.setScale(0, RoundingMode.UP); // làm tròn nghìn/lẻ tùy chính sách (đang làm tròn số nguyên)

        String breakdown = "base+" + req.getDistanceKm() + "km+" + chargeable + "kg";

        return PriceResponse.builder()
                .serviceTypeCode(st.getCode())
                .currency(st.getCurrency())
                .priceAmount(total)
                .basePrice(base)
                .distanceFee(distFee)
                .weightFee(weightFee)
                .chargeableWeightKg(chargeable)
                .breakdown(breakdown)
                .build();
    }
}
