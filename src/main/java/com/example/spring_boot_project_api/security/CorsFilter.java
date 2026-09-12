package com.example.spring_boot_project_api.security;

import java.io.IOException;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

public class CorsFilter extends OncePerRequestFilter {

    private static final String[] ALLOWED_ORIGINS = {
        "http://localhost:*",
        "https://*.trycloudflare.com",
        "https://*.vercel.app"
    };

    private static final Pattern[] ALLOWED_PATTERNS;

    static {
        ALLOWED_PATTERNS = new Pattern[ALLOWED_ORIGINS.length];
        for (int i = 0; i < ALLOWED_ORIGINS.length; i++) {
            String pattern = ALLOWED_ORIGINS[i]
                .replace(".", "\\.")
                .replace("*", ".*");
            ALLOWED_PATTERNS[i] = Pattern.compile(pattern);
        }
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isEmpty()) {
            if (isAllowedOrigin(origin)) {
                response.setHeader("Access-Control-Allow-Origin", origin);
                response.setHeader("Vary", "Origin");
                response.setHeader("Access-Control-Allow-Credentials", "true");
            }
        }
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, PATCH, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Authorization, Content-Type, Accept, Origin, X-Requested-With");
        response.setHeader("Access-Control-Max-Age", "3600");

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    private boolean isAllowedOrigin(String origin) {
        for (Pattern pattern : ALLOWED_PATTERNS) {
            if (pattern.matcher(origin).matches()) {
                return true;
            }
        }
        return false;
    }
}