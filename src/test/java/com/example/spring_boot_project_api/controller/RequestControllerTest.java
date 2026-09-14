package com.example.spring_boot_project_api.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.example.spring_boot_project_api.dto.request.RequestRequest;
import com.example.spring_boot_project_api.dto.response.RequestResponse;
import com.example.spring_boot_project_api.exception.GlobalExceptionHandler;
import com.example.spring_boot_project_api.service.RequestService;

@ExtendWith(MockitoExtension.class)
class RequestControllerTest {

    @Mock
    private RequestService requestService;

    @InjectMocks
    private RequestController requestController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(requestController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void loginAsCustomer() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .claim("uid", 1L)
                .claim("role", "CUSTOMER")
                .subject("test@example.com")
                .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")), "test@example.com");
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    void submit_returnsCreated() throws Exception {
        loginAsCustomer();
        RequestResponse response = new RequestResponse();
        response.setId(1L);
        response.setStatus("PENDING");
        when(requestService.submit(any(Long.class), any(String.class), any(RequestRequest.class))).thenReturn(response);

        String body = """
                {"type":"exam","notes":"Need eye checkup"}
                """;

        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void approve_returnsOk() throws Exception {
        RequestResponse response = new RequestResponse();
        response.setId(1L);
        response.setStatus("APPROVED");
        when(requestService.approve(1L)).thenReturn(response);

        mockMvc.perform(post("/api/requests/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void reject_returnsOk() throws Exception {
        RequestResponse response = new RequestResponse();
        response.setId(1L);
        response.setStatus("REJECTED");
        when(requestService.reject(1L)).thenReturn(response);

        mockMvc.perform(post("/api/requests/1/reject"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void getById_returnsOk() throws Exception {
        RequestResponse response = new RequestResponse();
        response.setId(5L);
        when(requestService.getById(5L)).thenReturn(response);

        mockMvc.perform(get("/api/requests/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/requests/5"))
                .andExpect(status().isNoContent());
    }
}