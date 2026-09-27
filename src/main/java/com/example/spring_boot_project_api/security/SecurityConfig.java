package com.example.spring_boot_project_api.security;

import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.MediaType;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtService jwtService;

    public SecurityConfig(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CorsFilter corsFilter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                // The Google endpoints must stay reachable while signed out, otherwise
                // a stale token in localStorage makes the bearer filter reject the
                // sign-in attempt itself. The frontend likewise omits the
                // Authorization header for these paths.
                .requestMatchers("/api/auth/login",
                        "/api/auth/google",
                        "/api/auth/google/exists",
                        "/api/customers/register").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/qr/**",
                        "/request-form", "/request-form/", "/request-form.html", "/request-form/**",
                        "/qr-dashboard", "/qr-dashboard/", "/qr-dashboard.html", "/qr-dashboard/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products",
                        "/api/products/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/attachments/by-product/**").permitAll()
                // Self-scoped: the caller is the subject, taken from the token.
                // /me must be declared before /{id} or the id path would shadow it.
                .requestMatchers(HttpMethod.GET, "/api/customers/me").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/customers/complete-profile")
                        .hasAnyRole("CUSTOMER")
                .requestMatchers(HttpMethod.GET, "/api/requests/mine").hasAnyRole("CUSTOMER")
                // Wishlist. The owner is resolved from the token inside
                // FavoriteService, so no request can name another customer; the
                // role rule here just keeps staff accounts out of a customer
                // feature.
                .requestMatchers("/api/favorites/**").hasAnyRole("CUSTOMER")
                .requestMatchers(HttpMethod.GET, "/api/requests/customer/**",
                        "/api/requests/status/**").hasAnyRole("STAFF", "ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/requests").hasAnyRole("STAFF", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/requests/**").hasAnyRole("STAFF", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/requests/{id}/approve").hasAnyRole("STAFF", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/requests/{id}/reject").hasAnyRole("STAFF", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/requests").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth
                .jwt(jwt -> jwt.jwtAuthenticationConverter(roleClaimConverter()))
                .authenticationEntryPoint(unauthorizedEntryPoint()))
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint(unauthorizedEntryPoint())
                .accessDeniedHandler(forbiddenAccessDeniedHandler()));

        return http.build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher("/swagger-ui/**", "/swagger-ui.html",
                    "/v3/api-docs", "/v3/api-docs/**",
                    "/swagger-resources/**", "/webjars/**")
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        SecretKey key = jwtService.getSecretKey();
        return NimbusJwtDecoder.withSecretKey(key).build();
    }

    private Converter<Jwt, ? extends AbstractAuthenticationToken> roleClaimConverter() {
        return jwt -> {
            String role = jwt.getClaimAsString("role");
            List<GrantedAuthority> authorities = List.of(
                    new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "STAFF")));
            return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
        };
    }

    private AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\": \"Unauthorized: token is missing or invalid\"}");
        };
    }

    private AccessDeniedHandler forbiddenAccessDeniedHandler() {
        return (HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\": \"Forbidden: you do not have permission to perform this action\"}");
        };
    }
}
