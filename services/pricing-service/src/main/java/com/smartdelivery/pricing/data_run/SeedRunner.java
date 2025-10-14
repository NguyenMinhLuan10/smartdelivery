package com.smartdelivery.pricing.data_run;

import com.smartdelivery.pricing.model.ServiceType;
import com.smartdelivery.pricing.repository.ServiceTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class SeedRunner implements CommandLineRunner {

    private final ServiceTypeRepository repo;

    @Override public void run(String... args) {
        if (!repo.existsByCode("STANDARD")) {
            repo.save(ServiceType.builder()
                    .code("STANDARD").name("Nội thành tiêu chuẩn")
                    .active(true)
                    .basePrice(new BigDecimal("15000"))
                    .perKmPrice(new BigDecimal("3500"))
                    .perKgPrice(new BigDecimal("3000"))
                    .volumetricDivisor(5000)
                    .currency("VND")
                    .slaHours(6)
                    .build());
        }
        if (!repo.existsByCode("SAME_DAY")) {
            repo.save(ServiceType.builder()
                    .code("SAME_DAY").name("Giao trong ngày")
                    .active(true)
                    .basePrice(new BigDecimal("25000"))
                    .perKmPrice(new BigDecimal("4500"))
                    .perKgPrice(new BigDecimal("3500"))
                    .volumetricDivisor(5000)
                    .currency("VND")
                    .slaHours(4)
                    .build());
        }
    }
}
