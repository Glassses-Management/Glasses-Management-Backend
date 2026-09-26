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

    private static final String[] STAFF = { "ADMIN", "STAFF", "OPTOMETRIST" };
    private static final String[] ADMIN_STAFF = { "ADMIN", "STAFF" };
    private static final String[] ADMIN_ONLY = { "ADMIN" };

    // Route patterns are kept as constants so SecurityConfigPatternsTest can
    // parse every one of them. An invalid pattern (for example a "**" in the
    // middle) only fails when the filter chain is built at startup, which is
    // well after compilation, so without that test a typo reaches the log as an
    // "Invalid mapping pattern" crash instead of a failing build.

    /** Reachable with no token at all. */
    static final String[] PUBLIC_PATHS = {
            "/api/auth/login",
            "/api/auth/google",
            "/api/auth/google/exists",
            "/api/customers/register",
    };

    /** Readable by anyone, including signed-out visitors. */
    static final String[] PUBLIC_GET_PATHS = {
            "/api/qr/**",
            "/request-form", "/request-form/", "/request-form.html", "/request-form/**",
            "/qr-dashboard", "/qr-dashboard/", "/qr-dashboard.html", "/qr-dashboard/**",
            "/api/products", "/api/products/**",
            "/api/attachments/by-product/**",
    };

    /** The caller's own records. Ownership comes from the token, not the path. */
    static final String[] SELF_PATHS = {
            "/api/customers/me",
            "/api/orders/mine",
            "/api/appointments/mine",
            "/api/prescriptions/mine",
            "/api/attachments/mine",
    };

    /** Signed-in account's own settings. */
    static final String[] ACCOUNT_PATHS = {
            "/api/auth/me",
            "/api/customers/complete-profile",
    };

    /** Customer's own order placement and avatar. Owner forced by the service. */
    static final String[] CUSTOMER_WRITABLE_PATHS = {
            "/api/orders",
            "/api/attachments/upload",
            "/api/attachments/*",
    };

    /**
     * Attachment routes reserved for staff.
     *
     * The user id is a single path variable, so each level needs "*" rather than
     * "**": "/api/users/**\/attachments/**" is rejected outright by
     * PathPatternParser because "**" is only valid at the start or end.
     */
    static final String[] ATTACHMENT_STAFF_PATHS = {
            "/api/product-attachments/**",
            "/api/users/*/attachments",
            "/api/users/*/attachments/*",
            "/api/user-attachments/**",
    };

    /** Staff-only record families. */
    static final String[] STAFF_RECORD_PATHS = {
            "/api/customers/**",
            "/api/prescriptions/**",
            "/api/appointments/**",
            "/api/inventories/**",
            "/api/orders/**",
            "/api/orderitems/**",
            "/api/attachments/**",
            "/api/products/**",
    };

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http, CorsFilter corsFilter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .addFilterBefore(corsFilter, UsernamePasswordAuthenticationFilter.class)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // ---- preflight and public entry points ----
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers(PUBLIC_PATHS).permitAll()
                .requestMatchers(HttpMethod.GET, PUBLIC_GET_PATHS).permitAll()

                // ---- self-scoped: the caller is the subject, taken from the token ----
                .requestMatchers(HttpMethod.GET, SELF_PATHS).authenticated()

                // ---- signed-in account's own settings ----
                .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/auth/change-password").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/customers/complete-profile").authenticated()

                // ---- customer request queue ----
                .requestMatchers(HttpMethod.GET, "/api/requests/mine").hasAnyRole("CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/api/requests").hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                .requestMatchers("/api/requests/**").hasAnyRole(ADMIN_STAFF)

                // ---- staff directories: readable by staff, writable by admin only ----
                .requestMatchers(HttpMethod.GET, "/api/user", "/api/user/**").hasAnyRole(STAFF)
                .requestMatchers("/api/user/**").hasAnyRole(ADMIN_ONLY)

                // A customer may place their own order from the cart, upload and
                // delete their own avatar, and finish their own profile. Each of
                // those service methods forces the owner to the caller for
                // CUSTOMER, because the body or path would otherwise let a
                // customer act against somebody else's profile. Everything else
                // under these families stays staff-only.
                .requestMatchers(HttpMethod.POST, "/api/orders").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/attachments/upload").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/attachments/*").authenticated()

                // ---- catalogue and media: browsing is public, editing is staff ----
                .requestMatchers(ATTACHMENT_STAFF_PATHS).hasAnyRole(STAFF)
                .requestMatchers(STAFF_RECORD_PATHS).hasAnyRole(STAFF)

                // Anything not matched above stays closed rather than open.
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
