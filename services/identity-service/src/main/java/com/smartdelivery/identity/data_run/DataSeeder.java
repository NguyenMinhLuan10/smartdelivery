package com.smartdelivery.identity.data_run;

import com.smartdelivery.identity.model.Role;
import com.smartdelivery.identity.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {
    private final RoleRepository roles;

    @Override
    public void run(String... args) {
        seed("ADMIN","Quản trị hệ thống");
        seed("DISPATCHER","Điều phối viên");
        seed("DRIVER","Tài xế giao hàng");
        seed("CUSTOMER","Khách hàng");
    }

    private void seed(String code, String name) {
        roles.findByCode(code).orElseGet(() -> {
            Role r = Role.builder()
                    .code(code)
                    .name(name)
                    .isSystem(true)
                    .build();
            return roles.save(r);
        });
    }
}
