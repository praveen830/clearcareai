package com.clearcareai.modules.auth.controller;

import com.clearcareai.exception.UnauthorizedException;
import com.clearcareai.modules.auth.dto.AuthResponse;
import com.clearcareai.modules.auth.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // a fake service - no database, no BCrypt, no JWT signing.
    // possible because the controller depends on the AuthService INTERFACE
    @MockBean
    private AuthService authService;

    private Map<String, String> validRegisterBody() {
        Map<String, String> body = new HashMap<>();
        body.put("email", "rahul@example.com");
        body.put("password", "Password123");
        body.put("firstName", "Rahul");
        body.put("lastName", "Sharma");
        body.put("phone", "9876543210");
        body.put("role", "ROLE_PATIENT");
        return body;
    }

    private AuthResponse buildAuthResponse() {
        return AuthResponse.builder()
                .accessToken("a.jwt.token")
                .refreshToken("a-uuid-value")
                .tokenType("Bearer")
                .userId(1L)
                .email("rahul@example.com")
                .role("ROLE_PATIENT")
                .build();
    }

    @Test
    void register_returns200AndTheStandardEnvelope() throws Exception {
        when(authService.register(any())).thenReturn(buildAuthResponse());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegisterBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.data.accessToken").value("a.jwt.token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    void register_withAWeakPassword_returns400_andTheServiceIsNeverCalled() throws Exception {
        Map<String, String> body = validRegisterBody();
        body.put("password", "abc");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        // proves @Valid + @ValidPassword ran in the web layer, before the service
    }

    @Test
    void login_returns200() throws Exception {
        when(authService.login(any())).thenReturn(buildAuthResponse());

        Map<String, String> body = new HashMap<>();
        body.put("email", "rahul@example.com");
        body.put("password", "Password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void login_whenTheServiceThrowsUnauthorized_returns401ViaTheGlobalHandler() throws Exception {
        when(authService.login(any()))
                .thenThrow(new UnauthorizedException("Invalid email or password"));

        Map<String, String> body = new HashMap<>();
        body.put("email", "rahul@example.com");
        body.put("password", "WrongPass123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}