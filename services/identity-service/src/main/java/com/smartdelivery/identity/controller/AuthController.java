package com.smartdelivery.identity.controller;

import com.smartdelivery.identity.dto.*;
import com.smartdelivery.identity.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;

    // ---------- Register / Verify ----------
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String,Object> register(@Valid @RequestBody SignupCustomerRequest req) {
        auth.registerCustomer(req);
        return Map.of("status","INACTIVE","message","Verification email sent");
    }

    @PostMapping("/verify-email")
    public Map<String,Object> verifyEmail(@Valid @RequestBody VerifyEmailRequest req) {
        boolean ok = auth.verifyEmail(req);
        return Map.of("activated", ok);
    }

    @PostMapping("/resend-verification")
    public Map<String,Object> resend(@Valid @RequestBody ResendVerificationRequest req) {
        auth.resendVerification(req);
        return Map.of("resent", true);
    }

    // ---------- Login / 2FA ----------
    @PostMapping("/login")
    public Map<String,Object> login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @PostMapping("/2fa/verify")
    public TokenPairResponse verify2FA(@Valid @RequestBody TwoFaVerifyRequest req) {
        return auth.verify2FA(req.getEmail(), req.getCode());
    }

    // bật/tắt 2FA → cần đăng nhập
    @PostMapping("/enable-2fa")
    @PreAuthorize("isAuthenticated()")
    public Map<String,Object> enable2FA(Authentication authn) {
        UUID me = UUID.fromString(authn.getName());
        auth.enable2FA(me);
        return Map.of("twoFA", true, "message", "2FA has been enabled");
    }

    @PostMapping("/disable-2fa")
    @PreAuthorize("isAuthenticated()")
    public Map<String,Object> disable2FA(Authentication authn) {
        UUID me = UUID.fromString(authn.getName());
        auth.disable2FA(me);
        return Map.of("twoFA", false, "message", "2FA has been disabled");
    }

    // AuthController.java
    @PostMapping("/2fa/enable/request")
    @PreAuthorize("isAuthenticated()")
    public Map<String,Object> requestEnable2FA(Authentication authn) {
        UUID me = UUID.fromString(authn.getName());
        auth.requestEnable2FA(me);
        return Map.of("sent", true, "message", "OTP has been sent to your email");
    }

    @PostMapping("/2fa/enable/confirm")
    @PreAuthorize("isAuthenticated()")
    public Map<String,Object> confirmEnable2FA(Authentication authn,
                                               @Valid @RequestBody TwoFaVerifyRequest req) {
        UUID me = UUID.fromString(authn.getName());
        auth.confirmEnable2FA(me, req.getCode());
        return Map.of("twoFA", true, "message", "2FA has been enabled");
    }

    // ---------- Token / Logout ----------
    @PostMapping("/token/refresh")
    public TokenPairResponse refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return auth.refresh(req);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody RefreshTokenRequest req) {
        auth.logout(req.getRefreshToken());
    }

    // ---------- Forgot / Reset password ----------
    @PostMapping("/forgot-password")
    public Map<String,Object> forgot(@Valid @RequestBody ForgotPasswordRequest req) {
        auth.forgot(req);
        return Map.of("sent", true);
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reset(@Valid @RequestBody ResetPasswordRequest req) {
        auth.reset(req);
    }
}
