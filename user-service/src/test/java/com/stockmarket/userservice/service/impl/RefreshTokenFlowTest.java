package com.stockmarket.userservice.service.impl;

import com.stockmarket.userservice.dto.AuthResponse;
import com.stockmarket.userservice.dto.RefreshRequest;
import com.stockmarket.userservice.entity.Role;
import com.stockmarket.userservice.entity.User;
import com.stockmarket.userservice.entity.UserStatus;
import com.stockmarket.userservice.exception.InvalidRefreshTokenException;
import com.stockmarket.userservice.repository.UserRepository;
import com.stockmarket.userservice.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.lenient;

/**
 * Refresh with REAL tokens (a real JwtService, not a mock), so signature, expiry and token type are actually checked.
 */
@ExtendWith(MockitoExtension.class)
class RefreshTokenFlowTest {

    private static final String SECRET = "a-test-secret-that-is-long-enough-for-hs256-signing-0123456789";

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Spy
    private JwtService jwtService = new JwtService(SECRET, 15, 7, "stock-market-system");

    @InjectMocks
    private UserServiceImpl userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId("user-123");
        user.setEmail("test@example.com");
        user.setRole(Role.ADMIN);
        user.setStatus(UserStatus.ACTIVE);
        lenient().when(userRepository.findById("user-123")).thenReturn(Optional.of(user));
    }

    private AuthResponse refresh(String token) {
        return userService.refresh(new RefreshRequest(token));
    }

    @Test
    void validRefreshToken_givesNewAccessTokenWithTheUsersRoleAndEmail() {
        String refreshToken = jwtService.generateRefreshToken("user-123");

        AuthResponse response = refresh(refreshToken);

        assertEquals("user-123", response.getUserId());
        assertEquals("test@example.com", response.getEmail());
        String access = response.getAccessToken();
        assertEquals("ACCESS", jwtService.extractTokenType(access));
        assertEquals("ADMIN", jwtService.extractRole(access), "role comes from the database, not from the old token");
        assertEquals("user-123", jwtService.extractUserId(access));
        assertEquals(refreshToken, response.getRefreshToken(), "the refresh token is returned unchanged (not rotated)");
    }

    @Test
    void accessTokenCannotBeUsedAsRefreshToken() {
        String access = jwtService.generateAccessToken("user-123", "test@example.com", "USER");
        assertThrows(InvalidRefreshTokenException.class, () -> refresh(access));
    }

    @Test
    void expiredRefreshToken_isRejected() {
        JwtService expiring = new JwtService(SECRET, 15, -1, "stock-market-system"); // 7 days -> minus 1 day
        assertThrows(InvalidRefreshTokenException.class, () -> refresh(expiring.generateRefreshToken("user-123")));
    }

    @Test
    void tokenSignedWithAnotherSecret_isRejected() {
        JwtService other = new JwtService("another-secret-that-is-also-long-enough-for-hs256-9876543210", 15, 7, "stock-market-system");
        assertThrows(InvalidRefreshTokenException.class, () -> refresh(other.generateRefreshToken("user-123")));
    }

    @Test
    void garbageAndTamperedTokens_areRejected() {
        assertThrows(InvalidRefreshTokenException.class, () -> refresh("not-a-jwt"));
        String good = jwtService.generateRefreshToken("user-123");
        assertThrows(InvalidRefreshTokenException.class, () -> refresh(good.substring(0, good.length() - 3) + "abc"));
    }

    @Test
    void aPasswordResetEndsSessionsThatStartedBeforeIt() {
        String token = jwtService.generateRefreshToken("user-123"); // issued now

        user.setPasswordChangedAt(java.time.LocalDateTime.now().plusSeconds(10)); // reset happened after the token was issued
        assertThrows(InvalidRefreshTokenException.class, () -> refresh(token));

        user.setPasswordChangedAt(java.time.LocalDateTime.now().minusHours(1)); // reset was long before the token
        assertNotNull(refresh(token).getAccessToken());

        user.setPasswordChangedAt(null); // never reset
        assertNotNull(refresh(token).getAccessToken());
    }

    @Test
    void unknownUser_isRejectedWithTheSameError() {
        lenient().when(userRepository.findById("ghost")).thenReturn(Optional.empty());
        assertThrows(InvalidRefreshTokenException.class, () -> refresh(jwtService.generateRefreshToken("ghost")));
    }

    @Test
    void blockedUsers_cannotRefresh_butActiveAndLegacyNullStatusCan() {
        for (UserStatus blocked : new UserStatus[] {UserStatus.SUSPENDED, UserStatus.BLOCKED, UserStatus.BANNED, UserStatus.DELETED, UserStatus.INACTIVE}) {
            user.setStatus(blocked);
            assertThrows(InvalidRefreshTokenException.class, () -> refresh(jwtService.generateRefreshToken("user-123")), blocked.name());
        }
        user.setStatus(UserStatus.ACTIVE);
        assertNotNull(refresh(jwtService.generateRefreshToken("user-123")).getAccessToken());
        user.setStatus(null); // rows from before the status column
        assertNotNull(refresh(jwtService.generateRefreshToken("user-123")).getAccessToken());
    }
}
