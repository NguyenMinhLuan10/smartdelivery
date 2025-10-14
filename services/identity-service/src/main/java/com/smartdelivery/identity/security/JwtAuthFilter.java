package com.smartdelivery.identity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    @Value("${app.jwt.secret}")
    private String secret;

    private static final AntPathMatcher PM = new AntPathMatcher();

    // Chỉ các path này là public (không yêu cầu JWT)
    private static final String[] PUBLIC_PATHS = new String[] {
            "/auth/register",
            "/auth/verify-email",
            "/auth/resend-verification",
            "/auth/login",
            "/auth/2fa/verify",
            "/auth/token/refresh",
            "/auth/forgot-password",
            "/auth/reset-password",
            "/users/ping",
            "/actuator/**"
    };

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Key key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String sub = claims.getSubject();                 // userId (UUID)
                String role = (String) claims.get("role");        // CUSTOMER/ADMIN/...

                var auth = new UsernamePasswordAuthenticationToken(
                        sub, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception ignored) {
                // token lỗi thì coi như chưa đăng nhập
            }
        }
        chain.doFilter(req, res);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        for (String p : PUBLIC_PATHS) {
            if (PM.match(p, path)) return true;   // chỉ skip các path public
        }
        return false; // còn lại (bao gồm /auth/enable-2fa) phải đi qua filter
    }
}
