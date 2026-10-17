package com.stockmarket.userservice.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private static final String SECRET_KEY = "myVerySecureSecretKeyForJwtTokenGeneration123456789";
    private static final long ACCESS_TOKEN_EXPIRY_MINUTES = 15;
    private static final long REFRESH_TOKEN_EXPIRY_DAYS = 7;
    private static final String ISSUER = "stock-market-system";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET_KEY, ACCESS_TOKEN_EXPIRY_MINUTES, REFRESH_TOKEN_EXPIRY_DAYS, ISSUER);
    }

    @Test
    void testGenerateAccessToken_Valid() {
        String userId = "user-uuid-1001";
        String email = "trader@stockmarket.com";

        String token = jwtService.generateAccessToken(userId, email);

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals(userId, jwtService.extractUserId(token));
        assertEquals(email, jwtService.extractEmail(token));
        assertEquals("ACCESS", jwtService.extractTokenType(token));
        assertFalse(jwtService.isTokenExpired(token));
    }

    @Test
    void testGenerateAccessToken_CarriesRole() {
        String admin = jwtService.generateAccessToken("admin-1", "admin@stockmarket.com", "ADMIN");
        String user = jwtService.generateAccessToken("user-1", "trader@stockmarket.com");

        assertEquals("ADMIN", jwtService.extractRole(admin));
        assertEquals("USER", jwtService.extractRole(user));
    }

    @Test
    void testGenerateAccessToken_WithLongUserId() {
        Long userId = 1001L;
        String email = "trader@stockmarket.com";

        String token = jwtService.generateAccessToken(userId, email);

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals("1001", jwtService.extractUserId(token));
        assertEquals(email, jwtService.extractEmail(token));
    }

    @Test
    void testGenerateRefreshToken_Valid() {
        String userId = "user-uuid-1001";

        String refreshToken = jwtService.generateRefreshToken(userId);

        assertNotNull(refreshToken);
        assertTrue(jwtService.validateToken(refreshToken));
        assertEquals(userId, jwtService.extractUserId(refreshToken));
        assertNull(jwtService.extractEmail(refreshToken));
        assertEquals("REFRESH", jwtService.extractTokenType(refreshToken));
        assertFalse(jwtService.isTokenExpired(refreshToken));
    }

    @Test
    void testValidateToken_InvalidSignature() {
        String userId = "user-uuid-1001";
        String email = "trader@stockmarket.com";

        String token = jwtService.generateAccessToken(userId, email);
        String tamperedToken = token + "corrupted";

        assertFalse(jwtService.validateToken(tamperedToken));
    }

    @Test
    void testValidateToken_ExpiredToken() {
        // Construct an already expired token with same secret key
        String expiredToken = Jwts.builder()
                .subject("user-uuid-1001")
                .issuer(ISSUER)
                .issuedAt(new Date(System.currentTimeMillis() - 1000 * 60 * 60))
                .expiration(new Date(System.currentTimeMillis() - 1000 * 60))
                .signWith(Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();

        assertFalse(jwtService.validateToken(expiredToken));
        assertTrue(jwtService.isTokenExpired(expiredToken));
    }
}
