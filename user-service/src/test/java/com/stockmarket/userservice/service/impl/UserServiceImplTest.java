package com.stockmarket.userservice.service.impl;

import com.stockmarket.userservice.dto.AuthResponse;
import com.stockmarket.userservice.dto.RegisterRequest;
import com.stockmarket.userservice.entity.User;
import com.stockmarket.userservice.repository.UserRepository;
import com.stockmarket.userservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private UserServiceImpl userService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("John");
        registerRequest.setLastName("Doe");
        registerRequest.setInitialBalance(BigDecimal.valueOf(5000));
    }

    @Test
    void register_Success() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user-123");
            return user;
        });
        when(jwtService.generateAccessToken("user-123", "test@example.com", "USER")).thenReturn("mock-access-token");
        when(jwtService.generateRefreshToken("user-123")).thenReturn("mock-refresh-token");

        AuthResponse response = userService.register(registerRequest);

        assertNotNull(response);
        assertEquals("user-123", response.getUserId());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals(BigDecimal.valueOf(5000), response.getWalletBalance());
        assertEquals("mock-access-token", response.getAccessToken());
        assertEquals("mock-refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());

        verify(kafkaTemplate, times(1)).send(eq("user.registered"), eq("user-123"), any());
        verify(jwtService, times(1)).generateAccessToken("user-123", "test@example.com", "USER");
        verify(jwtService, times(1)).generateRefreshToken("user-123");
    }

    @Test
    void login_Success() {
        User user = new User();
        user.setId("user-123");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setWalletBalance(BigDecimal.valueOf(5000));

        when(userRepository.findByEmail("test@example.com")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtService.generateAccessToken("user-123", "test@example.com", "USER")).thenReturn("mock-access-token");
        when(jwtService.generateRefreshToken("user-123")).thenReturn("mock-refresh-token");

        com.stockmarket.userservice.dto.LoginRequest loginRequest = new com.stockmarket.userservice.dto.LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        AuthResponse response = userService.login(loginRequest);

        assertNotNull(response);
        assertEquals("user-123", response.getUserId());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("John", response.getFirstName());
        assertEquals("Doe", response.getLastName());
        assertEquals(BigDecimal.valueOf(5000), response.getWalletBalance());
        assertEquals("mock-access-token", response.getAccessToken());
        assertEquals("mock-refresh-token", response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());

        verify(jwtService, times(1)).generateAccessToken("user-123", "test@example.com", "USER");
        verify(jwtService, times(1)).generateRefreshToken("user-123");
    }

    @Test
    void getUserById_Success() {
        User user = new User();
        user.setId("user-123");
        user.setEmail("test@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setWalletBalance(BigDecimal.valueOf(5000));

        when(userRepository.findById("user-123")).thenReturn(java.util.Optional.of(user));

        var response = userService.getUserById("user-123");

        assertNotNull(response);
        assertEquals("user-123", response.getId());
        assertEquals("test@example.com", response.getEmail());
        assertEquals(BigDecimal.valueOf(5000), response.getWalletBalance());
    }

    @Test
    void getUserById_NotFound_ThrowsException() {
        when(userRepository.findById("non-existent")).thenReturn(java.util.Optional.empty());

        assertThrows(RuntimeException.class, () -> userService.getUserById("non-existent"));
    }

    @Test
    void addFunds_Success() {
        User user = new User();
        user.setId("user-123");
        user.setWalletBalance(BigDecimal.valueOf(5000));

        when(userRepository.findById("user-123")).thenReturn(java.util.Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = userService.addFunds("user-123", BigDecimal.valueOf(1500));

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(6500), response.getWalletBalance());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void deductFunds_Success() {
        User user = new User();
        user.setId("user-123");
        user.setWalletBalance(BigDecimal.valueOf(5000));

        when(userRepository.findById("user-123")).thenReturn(java.util.Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = userService.deductFunds("user-123", BigDecimal.valueOf(2000));

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(3000), response.getWalletBalance());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void deductFunds_InsufficientBalance_ThrowsException() {
        User user = new User();
        user.setId("user-123");
        user.setWalletBalance(BigDecimal.valueOf(1000));

        when(userRepository.findById("user-123")).thenReturn(java.util.Optional.of(user));

        assertThrows(RuntimeException.class, () -> userService.deductFunds("user-123", BigDecimal.valueOf(2000)));
    }

    @Test
    void creditFunds_Success() {
        User user = new User();
        user.setId("user-123");
        user.setWalletBalance(BigDecimal.valueOf(5000));

        when(userRepository.findById("user-123")).thenReturn(java.util.Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = userService.creditFunds("user-123", BigDecimal.valueOf(1000));

        assertNotNull(response);
        assertEquals(BigDecimal.valueOf(6000), response.getWalletBalance());
    }
}
