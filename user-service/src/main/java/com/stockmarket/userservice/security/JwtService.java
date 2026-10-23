package com.stockmarket.userservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String CLAIM_ROLE = "role";
    public static final String DEFAULT_ROLE = "USER";

    public enum TokenType {
        ACCESS,
        REFRESH
    }

    private final String secretKey;
    private final long accessTokenExpiryMinutes;
    private final long refreshTokenExpiryDays;
    private final String issuer;

    public JwtService(
            @Value("${jwt.secret:myVerySecureSecretKeyForJwtTokenGeneration123456789}") String secretKey,
            @Value("${jwt.access-token-expiry-minutes:15}") long accessTokenExpiryMinutes,
            @Value("${jwt.refresh-token-expiry-days:7}") long refreshTokenExpiryDays,
            @Value("${jwt.issuer:stock-market-system}") String issuer) {
        this.secretKey = secretKey;
        this.accessTokenExpiryMinutes = accessTokenExpiryMinutes;
        this.refreshTokenExpiryDays = refreshTokenExpiryDays;
        this.issuer = issuer;
    }

    /**
     * Derives a cryptographic SecretKey suitable for HMAC-SHA256 signing.
     */
    public SecretKey getSigningKey() {
        byte[] keyBytes = this.secretKey.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates an access token for a USER-role user.
     */
    public String generateAccessToken(String userId, String email) {
        return generateAccessToken(userId, email, DEFAULT_ROLE);
    }

    /**
     * Generates an access token valid for configured access token duration (default 15 minutes).
     * Contains userId, email, role and tokenType=ACCESS claims.
     */
    public String generateAccessToken(String userId, String email, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofMinutes(this.accessTokenExpiryMinutes));

        Map<String, Object> claims = Map.of(
                CLAIM_USER_ID, userId,
                CLAIM_EMAIL, email,
                CLAIM_ROLE, role,
                CLAIM_TOKEN_TYPE, TokenType.ACCESS.name()
        );

        return buildToken(claims, userId, now, expiry);
    }

    public String generateAccessToken(Long userId, String email) {
        return generateAccessToken(String.valueOf(userId), email);
    }

    /**
     * Generates a refresh token valid for configured refresh token duration (default 7 days).
     * Contains userId and tokenType=REFRESH claims (no email claim).
     */
    public String generateRefreshToken(String userId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofDays(this.refreshTokenExpiryDays));

        Map<String, Object> claims = Map.of(
                CLAIM_USER_ID, userId,
                CLAIM_TOKEN_TYPE, TokenType.REFRESH.name()
        );

        return buildToken(claims, userId, now, expiry);
    }

    public String generateRefreshToken(Long userId) {
        return generateRefreshToken(String.valueOf(userId));
    }

    /**
     * Core builder method using modern JJWT 0.12.x API.
     */
    private String buildToken(Map<String, Object> extraClaims, String subject, Instant issuedAt, Instant expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuer(this.issuer)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Validates signature, expiration, and payload integrity.
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.error("JWT token expired: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("Invalid JWT token format: {}", e.getMessage());
        } catch (SecurityException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
        } catch (JwtException e) {
            log.error("JWT parsing error: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Extracts the userId claim or subject from token.
     */
    public String extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object userIdObj = claims.get(CLAIM_USER_ID);
        if (userIdObj != null) {
            return String.valueOf(userIdObj);
        }
        return claims.getSubject();
    }

    /**
     * Extracts the email claim from token.
     */
    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_EMAIL, String.class));
    }

    /**
     * Extracts the role claim; tokens issued before roles existed are USER.
     */
    public String extractRole(String token) {
        String role = extractClaim(token, claims -> claims.get(CLAIM_ROLE, String.class));
        return role == null ? DEFAULT_ROLE : role;
    }

    /**
     * Extracts the token type (ACCESS or REFRESH).
     */
    public String extractTokenType(String token) {
        return extractClaim(token, claims -> claims.get(CLAIM_TOKEN_TYPE, String.class));
    }

    /**
     * Extracts the issued-at date from token (second precision).
     */
    public Date extractIssuedAt(String token) {
        return extractClaim(token, Claims::getIssuedAt);
    }

    /**
     * Extracts expiration date from token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Checks if token is expired.
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = extractExpiration(token);
            return expiration.before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * Generic claim resolver helper.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Parses and returns all claims signed with the configured secret key.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
