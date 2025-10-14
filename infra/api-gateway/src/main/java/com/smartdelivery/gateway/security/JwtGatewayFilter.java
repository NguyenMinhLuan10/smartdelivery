package com.smartdelivery.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Arrays;
import java.util.List;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE) // chạy sớm
public class JwtGatewayFilter implements GlobalFilter {

    private final Key key;
    private final List<String> publicPaths;
    private static final AntPathMatcher PM = new AntPathMatcher();

    public JwtGatewayFilter(Environment env) {
        String secret = env.getProperty("app.jwt.secret");
        if (secret == null) {
            throw new IllegalStateException("Missing property: app.jwt.secret");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        // Bind list từ YAML, fallback cứng nếu không bind được
        List<String> fromYaml = Binder.get(env)
                .bind("app.security.public-paths", Bindable.listOf(String.class))
                .orElse(null);

        if (fromYaml == null || fromYaml.isEmpty()) {
            this.publicPaths = Arrays.asList(
                    "/auth/register", "/auth/verify-email", "/auth/resend-verification",
                    "/auth/login", "/auth/2fa/verify", "/auth/token/refresh",
                    "/auth/forgot-password", "/auth/reset-password",
                    "/users/ping", "/actuator/**"
            );
        } else {
            this.publicPaths = fromYaml;
        }
        System.out.println("[JwtGatewayFilter] Loaded public paths: " + this.publicPaths);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        // Bypass preflight (CORS)
        HttpMethod method = exchange.getRequest().getMethod();
        if (HttpMethod.OPTIONS.equals(method)) {
            return chain.filter(exchange);
        }

        // Đảm bảo tất cả /auth/** là public (để login/verify không bị chặn)
        if (path.startsWith("/auth/") || isPublic(path)) {
            return chain.filter(exchange);
        }

        // Các đường dẫn còn lại cần Bearer
        String auth = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(auth) || !auth.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = auth.substring(7);
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(this.key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String userId = claims.getSubject();
            String role   = (String) claims.get("role");
            String name   = (String) claims.get("name");
            String email  = (String) claims.get("email");

            // Gắn header chuyển tiếp xuống service đích
            ServerHttpRequest mutatedReq = exchange.getRequest().mutate()
                    .header("X-User-Id", userId == null ? "" : userId)
                    .header("X-User-Role", role == null ? "" : role)
                    .header("X-User-Name", name == null ? "" : name)
                    .header("X-User-Email", email == null ? "" : email)
                    .build();

            ServerWebExchange mutatedEx = exchange.mutate().request(mutatedReq).build();
            return chain.filter(mutatedEx);

        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublic(String path) {
        for (String p : publicPaths) {
            if (PM.match(p, path)) return true;
        }
        return false;
    }
}
