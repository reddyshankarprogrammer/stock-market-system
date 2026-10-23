package com.stockmarket.userservice.service.impl;

import com.stockmarket.userservice.dto.AuthResponse;
import com.stockmarket.userservice.dto.ChangePasswordRequest;
import com.stockmarket.userservice.entity.UserStatus;
import com.stockmarket.userservice.exception.AccountNotActiveException;
import com.stockmarket.userservice.exception.IncorrectPasswordException;
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
    void register_WithProfileFields_SavesThemAndSetsAcceptanceTimeOnServer() {
        registerRequest.setMobileCountryCode("+91");
        registerRequest.setMobileNumber("9876543210");
        registerRequest.setGender("FEMALE");
        registerRequest.setCountry("IN");
        registerRequest.setTermsVersion("2026-10-24-draft");
        stubSuccessfulRegister();

        java.time.LocalDateTime before = java.time.LocalDateTime.now();
        userService.register(registerRequest);

        org.mockito.ArgumentCaptor<User> saved = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User user = saved.getValue();
        assertEquals("+91", user.getMobileCountryCode());
        assertEquals("9876543210", user.getMobileNumber());
        assertEquals(com.stockmarket.userservice.entity.Gender.FEMALE, user.getGender());
        assertEquals("IN", user.getCountry());
        assertEquals("2026-10-24-draft", user.getTermsVersion());
        assertNotNull(user.getTermsAcceptedAt(), "acceptance time is set by the server");
        assertFalse(user.getTermsAcceptedAt().isBefore(before));
    }

    @Test
    void register_WithoutProfileFields_StillWorksAndLeavesThemEmpty() {
        stubSuccessfulRegister();

        userService.register(registerRequest);

        org.mockito.ArgumentCaptor<User> saved = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        User user = saved.getValue();
        assertNull(user.getMobileNumber());
        assertNull(user.getGender());
        assertNull(user.getCountry());
        assertNull(user.getTermsVersion());
        assertNull(user.getTermsAcceptedAt(), "no terms version given, so no acceptance time");
    }

    @Test
    @SuppressWarnings("unchecked")
    void register_KafkaEventDoesNotCarryPersonalProfileFields() {
        registerRequest.setMobileCountryCode("+91");
        registerRequest.setMobileNumber("9876543210");
        registerRequest.setGender("MALE");
        registerRequest.setCountry("IN");
        stubSuccessfulRegister();

        userService.register(registerRequest);

        org.mockito.ArgumentCaptor<Object> event = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(kafkaTemplate).send(eq("user.registered"), eq("user-123"), event.capture());
        java.util.Map<String, Object> map = (java.util.Map<String, Object>) event.getValue();
        assertEquals(java.util.Set.of("userId", "email", "firstName", "lastName", "walletBalance"), map.keySet(),
                "the user.registered contract is unchanged; mobile, gender and country are not published");
    }

    private void stubSuccessfulRegister() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId("user-123");
            return user;
        });
        when(jwtService.generateAccessToken("user-123", "test@example.com", "USER")).thenReturn("mock-access-token");
        when(jwtService.generateRefreshToken("user-123")).thenReturn("mock-refresh-token");
    }

    private com.stockmarket.userservice.dto.LoginRequest loginRequest(String email, String password) {
        com.stockmarket.userservice.dto.LoginRequest r = new com.stockmarket.userservice.dto.LoginRequest();
        r.setEmail(email);
        r.setPassword(password);
        return r;
    }

    private User userWithStatus(com.stockmarket.userservice.entity.UserStatus status) {
        User user = new User();
        user.setId("user-123");
        user.setEmail("test@example.com");
        user.setPassword("encodedPassword");
        user.setStatus(status);
        return user;
    }

    @Test
    void login_UnknownEmail_IsTheSameAsAWrongPassword_AndStillDoesTheBcryptWork() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(java.util.Optional.empty());
        when(userRepository.findByEmail("test@example.com")).thenReturn(java.util.Optional.of(userWithStatus(null)));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        var unknown = assertThrows(com.stockmarket.userservice.exception.InvalidCredentialsException.class,
                () -> userService.login(loginRequest("nobody@example.com", "whatever1")));
        // a dummy comparison ran, so the response time does not reveal that the email is unregistered
        verify(passwordEncoder, times(1)).matches(eq("whatever1"), anyString());

        var wrong = assertThrows(com.stockmarket.userservice.exception.InvalidCredentialsException.class,
                () -> userService.login(loginRequest("test@example.com", "wrong-password")));

        assertEquals(wrong.getMessage(), unknown.getMessage(), "same message for unknown email and wrong password");
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_BlockedAccount_IsRefusedOnlyAfterTheCorrectPassword() {
        for (com.stockmarket.userservice.entity.UserStatus blocked : new com.stockmarket.userservice.entity.UserStatus[] {
                com.stockmarket.userservice.entity.UserStatus.SUSPENDED, com.stockmarket.userservice.entity.UserStatus.BANNED,
                com.stockmarket.userservice.entity.UserStatus.BLOCKED, com.stockmarket.userservice.entity.UserStatus.DELETED,
                com.stockmarket.userservice.entity.UserStatus.ACCOUNT_LOCKED, com.stockmarket.userservice.entity.UserStatus.INACTIVE}) {
            User user = userWithStatus(blocked);
            when(userRepository.findByEmail("test@example.com")).thenReturn(java.util.Optional.of(user));

            when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
            assertThrows(com.stockmarket.userservice.exception.AccountNotActiveException.class,
                    () -> userService.login(loginRequest("test@example.com", "password123")), blocked.name());

            // a wrong password never reveals the status
            when(passwordEncoder.matches("guess", "encodedPassword")).thenReturn(false);
            assertThrows(com.stockmarket.userservice.exception.InvalidCredentialsException.class,
                    () -> userService.login(loginRequest("test@example.com", "guess")), blocked.name());
        }
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_ActiveAndLegacyNullStatusUsers_CanStillLogIn() {
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtService.generateAccessToken(anyString(), anyString(), anyString())).thenReturn("access");
        when(jwtService.generateRefreshToken(anyString())).thenReturn("refresh");

        for (com.stockmarket.userservice.entity.UserStatus ok : new com.stockmarket.userservice.entity.UserStatus[] {
                com.stockmarket.userservice.entity.UserStatus.ACTIVE, null,
                com.stockmarket.userservice.entity.UserStatus.PENDING_VERIFICATION, com.stockmarket.userservice.entity.UserStatus.EMAIL_NOT_VERIFIED}) {
            User user = userWithStatus(ok);
            user.setRole(com.stockmarket.userservice.entity.Role.USER);
            when(userRepository.findByEmail("test@example.com")).thenReturn(java.util.Optional.of(user));

            assertEquals("access", userService.login(loginRequest("test@example.com", "password123")).getAccessToken(), String.valueOf(ok));
        }
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

    // ---- change password ----

    private User signedInUser() {
        User user = new User();
        user.setId("user-1");
        user.setEmail("test@example.com");
        user.setFirstName("John");
        user.setPassword("old-hash");
        return user;
    }

    @Test
    void changePassword_savesTheNewHash_marksTheChange_andReturnsFreshTokensForThisSession() {
        User user = signedInUser();
        when(userRepository.findById("user-1")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("OldPass123", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("NewPass456")).thenReturn("new-hash");
        when(jwtService.generateAccessToken("user-1", "test@example.com", "USER")).thenReturn("new-access");
        when(jwtService.generateRefreshToken("user-1")).thenReturn("new-refresh");

        AuthResponse response = userService.changePassword("user-1", new ChangePasswordRequest("OldPass123", "NewPass456"));

        assertEquals("new-hash", user.getPassword());
        assertNotNull(user.getPasswordChangedAt(), "older sessions must end");
        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_wrongCurrentPassword_isRefused_andNothingChanges() {
        User user = signedInUser();
        when(userRepository.findById("user-1")).thenReturn(java.util.Optional.of(user));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThrows(IncorrectPasswordException.class, () -> userService.changePassword("user-1", new ChangePasswordRequest("wrong", "NewPass456")));

        assertEquals("old-hash", user.getPassword());
        assertNull(user.getPasswordChangedAt());
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_theSamePassword_isRefused() {
        when(userRepository.findById("user-1")).thenReturn(java.util.Optional.of(signedInUser()));
        when(passwordEncoder.matches("SamePass123", "old-hash")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> userService.changePassword("user-1", new ChangePasswordRequest("SamePass123", "SamePass123")));
        verify(userRepository, never()).save(any());
    }

    @Test
    void changePassword_blockedAccount_isRefused_beforeThePasswordIsChecked() {
        User user = signedInUser();
        user.setStatus(UserStatus.SUSPENDED);
        when(userRepository.findById("user-1")).thenReturn(java.util.Optional.of(user));

        assertThrows(AccountNotActiveException.class, () -> userService.changePassword("user-1", new ChangePasswordRequest("OldPass123", "NewPass456")));
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }
}
