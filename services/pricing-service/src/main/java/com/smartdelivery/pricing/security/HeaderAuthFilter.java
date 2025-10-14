package com.smartdelivery.pricing.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class HeaderAuthFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PM = new AntPathMatcher();
    private static final String[] PUBLIC = {
            "/pricing/quote",
            "/actuator/**"
    };

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        for (String p : PUBLIC) {
            if (PM.match(p, path)) return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String uid  = req.getHeader("X-User-Id");
        String role = req.getHeader("X-User-Role"); // CUSTOMER/ADMIN/...

        if (uid != null && role != null) {
            var auth = new SimpleAuth(uid, role);
            SecurityContextHolder.getContext().setAuthentication(auth);
        }
        chain.doFilter(req, res);
    }

    static class SimpleAuth extends AbstractAuthenticationToken {
        private final String name;

        SimpleAuth(String userId, String role) {
            super(List.of(new SimpleGrantedAuthority("ROLE_" + role)));
            this.name = userId;
            setAuthenticated(true);
        }
        @Override public Object getCredentials() { return ""; }
        @Override public Object getPrincipal() { return name; }
        @Override public String getName() { return name; }
    }
}
