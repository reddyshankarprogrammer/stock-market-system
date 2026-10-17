package com.stockmarket.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockmarket.userservice.config.SecurityConfig;
import com.stockmarket.userservice.dto.AuthResponse;
import com.stockmarket.userservice.dto.LoginRequest;
import com.stockmarket.userservice.dto.RegisterRequest;
import com.stockmarket.userservice.dto.UserResponse;
import com.stockmarket.userservice.entity.Role;
import com.stockmarket.userservice.entity.UserStatus;
import com.stockmarket.userservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @Test
    void register_ValidRequest_ReturnsCreated() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("user@example.com");
        request.setPassword("SecurePass123!");
        request.setFirstName("John");
        request.setLastName("Doe");

        AuthResponse authResponse = new AuthResponse();
        authResponse.setUserId("user-1");
        authResponse.setEmail("user@example.com");
        authResponse.setAccessToken("token-123");

        when(userService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.accessToken").value("token-123"));
    }

    @Test
    void login_ValidRequest_ReturnsOk() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("SecurePass123!");

        AuthResponse authResponse = new AuthResponse();
        authResponse.setUserId("user-1");
        authResponse.setEmail("user@example.com");
        authResponse.setAccessToken("token-123");

        when(userService.login(any(LoginRequest.class))).thenReturn(authResponse);

        mockMvc.perform(post("/api/v1/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.accessToken").value("token-123"));
    }

    @Test
    void getUserById_ReturnsOk() throws Exception {
        UserResponse response = new UserResponse("user-1", "user@example.com", "John", "Doe", BigDecimal.valueOf(5000), UserStatus.ACTIVE, LocalDateTime.now(), Role.USER);

        when(userService.getUserById("user-1")).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/user-1").header("X-User-Id", "user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-1"))
                .andExpect(jsonPath("$.walletBalance").value(5000));
    }

    @Test
    void getUserById_OtherUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/users/user-2").header("X-User-Id", "user-1"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void getUserById_OtherUserAsAdmin_ReturnsOk() throws Exception {
        UserResponse response = new UserResponse("user-2", "other@example.com", "Jane", "Roe", BigDecimal.valueOf(100), UserStatus.ACTIVE, LocalDateTime.now(), Role.USER);
        when(userService.getUserById("user-2")).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/user-2").header("X-User-Id", "admin-1").header("X-User-Role", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-2"));
    }

    @Test
    void getUserById_MissingUserHeader_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/user-1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addFunds_ReturnsOk() throws Exception {
        UserResponse response = new UserResponse("user-1", "user@example.com", "John", "Doe", BigDecimal.valueOf(6000), UserStatus.ACTIVE, LocalDateTime.now(), Role.USER);

        when(userService.addFunds(eq("user-1"), any(BigDecimal.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/users/user-1/funds/add")
                        .header("X-User-Id", "user-1")
                        .param("amount", "1000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletBalance").value(6000));
    }

    @Test
    void addFunds_OtherUser_ReturnsForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/users/user-2/funds/add")
                        .header("X-User-Id", "user-1")
                        .param("amount", "1000"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }
}
