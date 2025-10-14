package com.smartdelivery.identity.service;

import com.smartdelivery.identity.client.DispatchSyncClient;
import com.smartdelivery.identity.dto.UpdateStatusRequest;
import com.smartdelivery.identity.dto.UserResponse;
import com.smartdelivery.identity.model.Role;
import com.smartdelivery.identity.model.User;
import com.smartdelivery.identity.repository.RoleRepository;
import com.smartdelivery.identity.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final RoleRepository roles;
    private final DispatchSyncClient dispatchSyncClient;

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponse> list(Pageable pageable){
        return users.findAll(pageable).map(u -> UserResponse.builder()
                .id(u.getId()).roleCode(u.getRoleCode()).email(u.getEmail())
                .phone(u.getPhone()).name(u.getName()).status(u.getStatus())
                .createdAt(u.getCreatedAt()).build());
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public void updateStatus(UUID id, UpdateStatusRequest req){
        User u = users.findById(id).orElseThrow();
        u.setStatus(req.getStatus());
        users.save(u);
    }

    @Transactional
    public void changeMyPassword(UUID me, String oldPwd, String newPwd){
        User u = users.findById(me).orElseThrow();
        if (!encoder.matches(oldPwd, u.getPasswordHash()))
            throw new IllegalArgumentException("Old password mismatch");
        u.setPasswordHash(encoder.encode(newPwd));
        users.save(u);
    }


    // identity-service: service/UserService.java
    @Transactional
    public UUID updateRole(UUID id, String roleCode, String authHeader) {
        var u = users.findById(id).orElseThrow();
        Role role = roles.findByCode(roleCode)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleCode));
        u.setRole(role);
        u.setRoleCode(roleCode);
        users.save(u);

        if ("DRIVER".equalsIgnoreCase(roleCode)) {
            try {
                return dispatchSyncClient.syncDriver(u.getId(), u.getName(), u.getPhone(), u.getEmail(), authHeader);
            } catch (Exception ex) {
                System.err.println("Sync driver to dispatch failed: " + ex.getMessage());
                // tuỳ bạn: ném lỗi hoặc trả null
            }
        }
        return null;
    }


}
