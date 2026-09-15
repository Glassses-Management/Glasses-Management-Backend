package com.example.spring_boot_project_api.security;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

public class CorsFilter extends OncePerRequestFilter {

    private static final Pattern[] ALLOWED_PATTERNS;

    static {
        // Patterns: localhost any port, trycloudflare, vercel
        String[] rawPatterns = {
            "http://localhost:.*",
            "https://.*\\.trycloudflare\\.com",
            "https://.*\\.vercel\\.app"
        };
        ALLOWED_PATTERNS = new Pattern[rawPatterns.length];
        for (int i = 0; i < rawPatterns.length; i++) {
            ALLOWED_PATTERNS[i] = Pattern.compile(rawPatterns[i]);
        }
    }

    private static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String origin = request.getHeader("Origin");
        if (origin != null && !origin.isEmpty() && isAllowedOrigin(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Vary", "Origin");
            response.setHeader("Access-Control-Allow-Credentials", "true");
        }
        response.setHeader("Access-Control-Allow-Methods", String.join(", ", ALLOWED_METHODS));
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