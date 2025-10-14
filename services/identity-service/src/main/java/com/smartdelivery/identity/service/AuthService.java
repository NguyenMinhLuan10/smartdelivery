package com.smartdelivery.identity.service;

import com.google.gson.Gson;
import com.smartdelivery.identity.dto.*;
import com.smartdelivery.identity.model.*;
import com.smartdelivery.identity.repository.*;
import com.smartdelivery.identity.security.JwtUtil;
import com.smartdelivery.identity.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final RoleRepository roles;
    private final OtpTokenRepository otpRepo;
    private final AuditAuthRepository auditRepo;
    private final RefreshTokenRepository rtRepo;
    private final PasswordEncoder encoder;
    private final JwtUtil jwt;
    private final EmailService emailService;
    private final Gson gson = new Gson();

    // ===== REGISTER & VERIFY =====

    @Transactional
    public void registerCustomer(SignupCustomerRequest req){
        String email = req.getEmail().trim().toLowerCase();
        if (users.existsByEmail(email))  throw new IllegalArgumentException("Email already used");
        if (users.existsByPhone(req.getPhone())) throw new IllegalArgumentException("Phone already used");
        roles.findByCode("CUSTOMER").orElseThrow(() -> new IllegalStateException("Role CUSTOMER missing"));

        User u = User.builder()
                .roleCode("CUSTOMER")
                .email(email)
                .phone(req.getPhone())
                .name(req.getName())
                .passwordHash(encoder.encode(req.getPassword()))
                .status("INACTIVE")
                .twofaEnabled(false)
                .locale("vi").tz("Asia/Ho_Chi_Minh")
                .build();
        users.save(u);

        sendRegisterOtp(u);
        audit(u.getId(), "REGISTERED", "USER", u.getId(), Map.of("email",email));
    }

    @Transactional
    public void resendVerification(ResendVerificationRequest req){
        User u = users.findByEmail(req.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if ("ACTIVE".equals(u.getStatus())) return;
        sendRegisterOtp(u);
    }

    @Transactional
    public boolean verifyEmail(VerifyEmailRequest req){
        User u = users.findByEmail(req.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại"));
        if ("ACTIVE".equals(u.getStatus())) return true;

        String hashed = CryptoUtil.hmacOtp(req.getCode().trim());
        var valids = otpRepo.findValid(u.getId(), "REGISTER", Instant.now());
        OtpToken hit = valids.stream().filter(v -> hashed.equals(v.getOtpHash())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Mã xác nhận sai hoặc hết hạn"));

        hit.setUsedAt(Instant.now());
        otpRepo.save(hit);

        u.setStatus("ACTIVE");
        users.save(u);
        audit(u.getId(), "EMAIL_VERIFIED", "USER", u.getId(), Map.of());
        return true;
    }

    private void sendRegisterOtp(User u){
        otpRepo.findValid(u.getId(), "REGISTER", Instant.now())
                .forEach(o -> { o.setExpiresAt(Instant.now()); otpRepo.save(o); });

        String otp = sixDigits();
        String token = Base64.getUrlEncoder().withoutPadding()
                .encodeToString((u.getId()+":"+otp+":"+System.currentTimeMillis()).getBytes());

        OtpToken row = OtpToken.builder()
                .userId(u.getId())
                .kind("REGISTER")
                .otpHash(CryptoUtil.hmacOtp(otp))
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .build();
        otpRepo.save(row);

        emailService.sendVerifyEmail(u, otp, token);
        audit(u.getId(), "EMAIL_SENT", "OTP", row.getId(), Map.of("kind","REGISTER"));
    }

    // ===== LOGIN / 2FA =====

    @Transactional
    public Map<String,Object> login(LoginRequest req){
        var u = users.findByEmail(req.getUsername())
                .or(() -> users.findByPhone(req.getUsername()))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!"ACTIVE".equals(u.getStatus())) throw new IllegalStateException("Account is not ACTIVE");
        if (!encoder.matches(req.getPassword(), u.getPasswordHash()))
            throw new IllegalArgumentException("Invalid credentials");

        if (Boolean.TRUE.equals(u.getTwofaEnabled())) {
            sendTwoFaOtp(u);
            audit(u.getId(), "LOGIN_REQUIRE_2FA", "USER", u.getId(), Map.of());
            return Map.of("require2FA", true);
        }

        TokenPairResponse pair = issueTokenPair(u);
        u.setLastLoginAt(Instant.now());
        users.save(u);
        audit(u.getId(), "LOGIN", "USER", u.getId(), Map.of("twoFA", false));
        return Map.of("require2FA", false, "tokens", pair);
    }

    @Transactional
    public TokenPairResponse verify2FA(String email, String code){
        User u = users.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String hashed = CryptoUtil.hmacOtp(code.trim());
        var valids = otpRepo.findValid(u.getId(), "2FA", Instant.now());
        OtpToken hit = valids.stream().filter(v -> hashed.equals(v.getOtpHash())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("OTP 2FA sai/hết hạn"));

        hit.setUsedAt(Instant.now());
        otpRepo.save(hit);

        TokenPairResponse pair = issueTokenPair(u);
        u.setLastLoginAt(Instant.now());
        users.save(u);
        audit(u.getId(), "LOGIN", "USER", u.getId(), Map.of("twoFA", true));
        return pair;
    }

    public void enable2FA(UUID userId){
        User u = users.findById(userId).orElseThrow();
        u.setTwofaEnabled(true);
        users.save(u);
        audit(userId, "2FA_ENABLED", "USER", userId, Map.of());
    }

    public void disable2FA(UUID userId){
        User u = users.findById(userId).orElseThrow();
        u.setTwofaEnabled(false);
        users.save(u);
        audit(userId, "2FA_DISABLED", "USER", userId, Map.of());
    }

    private void sendTwoFaOtp(User u){
        otpRepo.findValid(u.getId(), "2FA", Instant.now())
                .forEach(o -> { o.setExpiresAt(Instant.now()); otpRepo.save(o); });

        String otp = sixDigits();
        OtpToken row = OtpToken.builder()
                .userId(u.getId()).kind("2FA")
                .otpHash(CryptoUtil.hmacOtp(otp))
                .expiresAt(Instant.now().plus(10, ChronoUnit.MINUTES))
                .build();
        otpRepo.save(row);
        emailService.sendTwoFaEmail(u, otp);
    }

    // ===== REFRESH / LOGOUT =====

    @Transactional
    public TokenPairResponse refresh(RefreshTokenRequest req){
        RefreshToken old = rtRepo.findByToken(req.getRefreshToken())
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));
        if (!old.isActive(Instant.now())) throw new IllegalArgumentException("Refresh token expired/revoked");

        old.setRevokedAt(Instant.now());
        rtRepo.save(old);

        User u = users.findById(old.getUserId()).orElseThrow();
        TokenPairResponse pair = issueTokenPair(u);
        audit(u.getId(), "REFRESH", "REFRESH", old.getId(), Map.of("rotated", true));
        return pair;
    }

    @Transactional
    public void logout(String refreshToken){
        rtRepo.findByToken(refreshToken).ifPresent(rt -> {
            rt.setRevokedAt(Instant.now());
            rtRepo.save(rt);
            audit(rt.getUserId(), "LOGOUT", "REFRESH", rt.getId(), Map.of());
        });
    }

    // ===== FORGOT / RESET =====

    @Transactional
    public void forgot(ForgotPasswordRequest req){
        User u = users.findByEmail(req.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại"));
        otpRepo.findValid(u.getId(), "RESET", Instant.now())
                .forEach(o -> { o.setExpiresAt(Instant.now()); otpRepo.save(o); });

        String otp = sixDigits();
        OtpToken row = OtpToken.builder()
                .userId(u.getId()).kind("RESET")
                .otpHash(CryptoUtil.hmacOtp(otp))
                .expiresAt(Instant.now().plus(30, ChronoUnit.MINUTES))
                .build();
        otpRepo.save(row);
        emailService.sendResetPasswordEmail(u, otp);
        audit(u.getId(), "RESET_SENT", "OTP", row.getId(), Map.of());
    }

    @Transactional
    public void reset(ResetPasswordRequest req){
        User u = users.findByEmail(req.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Email không tồn tại"));
        String hashed = CryptoUtil.hmacOtp(req.getCode().trim());
        var valids = otpRepo.findValid(u.getId(), "RESET", Instant.now());
        OtpToken hit = valids.stream().filter(v -> hashed.equals(v.getOtpHash())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("OTP sai/hết hạn"));

        hit.setUsedAt(Instant.now());
        otpRepo.save(hit);

        u.setPasswordHash(encoder.encode(req.getNewPassword()));
        users.save(u);
        audit(u.getId(), "PASSWORD_RESET", "USER", u.getId(), Map.of());

        // revoke toàn bộ refresh tokens hiện hữu
        rtRepo.deleteByUserId(u.getId());
    }

    // ===== Helpers =====

    private TokenPairResponse issueTokenPair(User u){
        String access = jwt.generate(u.getId().toString(),
                Map.of("role", u.getRoleCode(),
                        "name", u.getName(),
                        "email", u.getEmail()));
        long accessTtl = jwt.getTtlSeconds();

        // refresh: 30 ngày (theo chuẩn nội bộ, không cần YAML)
        long refreshTtlSec = 30L * 24 * 3600;
        String refresh = UUID.randomUUID() + "." + UUID.randomUUID();

        RefreshToken rt = RefreshToken.builder()
                .userId(u.getId())
                .token(refresh)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plus(refreshTtlSec, ChronoUnit.SECONDS))
                .build();
        rtRepo.save(rt);

        return new TokenPairResponse(access, accessTtl, refresh, refreshTtlSec);
    }

    private void audit(UUID actor, String action, String target, UUID targetId, Map<String,Object> meta){
        auditRepo.save(AuditAuth.builder()
                .actorUserId(actor).action(action).target(target).targetId(targetId)
                .metaJson(gson.toJson(meta)).build());
    }

    private static String sixDigits(){
        return String.format("%06d", new Random().nextInt(1_000_000));
    }
}
