package com.smartdelivery.identity.controller;

import com.smartdelivery.identity.dto.ChangePasswordRequest;
import com.smartdelivery.identity.dto.UpdateRoleRequest;
import com.smartdelivery.identity.dto.UpdateStatusRequest;
import com.smartdelivery.identity.dto.UserResponse;
import com.smartdelivery.identity.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService users;

    @GetMapping("/ping")
    public Map<String,String> ping() {
        return Map.of("ok","true");
    }

    // chỉ ADMIN mới được list user
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponse> list(Pageable pageable) {
        return users.list(pageable);
    }

    // user tự đổi mật khẩu của mình
    @PostMapping("/me/change-password")
    @PreAuthorize("isAuthenticated()")
    public Map<String,Object> changeMyPassword(Authentication auth,
                                               @Valid @RequestBody ChangePasswordRequest req) {
        UUID me = UUID.fromString(auth.getName()); // subject = userId trong JWT
        users.changeMyPassword(me, req.getOldPwd(), req.getNewPwd());
        return Map.of("changed", true);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String,Object> updateRole(@PathVariable("id") UUID id,
                                         @Valid @RequestBody UpdateRoleRequest req,
                                         @RequestHeader(value="Authorization", required=false) String authHeader) {
        UUID driverId = users.updateRole(id, req.getRole(), authHeader);

        if ("DRIVER".equalsIgnoreCase(req.getRole()) && driverId != null) {
            return Map.of("updated", true, "role", req.getRole(), "driverId", driverId);
        }
        return Map.of("updated", true, "role", req.getRole());
    }


    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String,Object> updateStatus(@PathVariable("id") UUID id,
                                           @Valid @RequestBody UpdateStatusRequest req) {
        users.updateStatus(id, req);
        return Map.of("updated", true);
    }


}
