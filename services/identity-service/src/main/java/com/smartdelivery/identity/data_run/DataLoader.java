package com.smartdelivery.identity.data_run;

import com.smartdelivery.identity.model.Role;
import com.smartdelivery.identity.model.User;
import com.smartdelivery.identity.repository.RoleRepository;
import com.smartdelivery.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataLoader {

    private final RoleRepository roles;
    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Bean
    ApplicationRunner init() {
        return args -> {
            // Lấy role ADMIN sau khi DataSeeder đã chạy
            Role adminRole = roles.findByCode("ADMIN").orElseThrow();

            // Tạo tài khoản admin mặc định nếu chưa có
            users.findByEmail("admin@smartdelivery.local").orElseGet(() ->
                    users.save(User.builder()
                            .role(adminRole)
                            .roleCode(adminRole.getCode())
                            .email("admin@smartdelivery.local")
                            .phone("0900000000")
                            .name("System Admin")
                            .passwordHash(encoder.encode("Admin@123"))
                            .status("ACTIVE")
                            .build())
            );
        };
    }
}
