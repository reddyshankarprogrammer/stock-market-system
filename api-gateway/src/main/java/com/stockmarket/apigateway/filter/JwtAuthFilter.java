package com.stockmarket.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Validates the JWT access token issued by user-service on every /api/** request
 * and forwards the caller's identity downstream as X-User-Id / X-User-Email / X-User-Role headers.
 *
 * Runs as a servlet filter because the gateway uses Spring Cloud Gateway server-webmvc.
 * Any X-User-* headers sent by the client are stripped so they can't be spoofed.
 */
@Slf4j
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_EMAIL_HEADER = "X-User-Email";
    public static final String USER_ROLE_HEADER = "X-User-Role";

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String IDENTITY_HEADER_PREFIX = "x-user-";

    // Claim names and token type match user-service's JwtService.
    private static final String CLAIM_USER_ID = "userId";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String CLAIM_ROLE = "role";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String DEFAULT_ROLE = "USER";
    private static final String ADMIN_ROLE = "ADMIN";

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/users/register",
            "/api/v1/users/login",
            "/api/v1/users/refresh", // the refresh token is in the body; the expired access token cannot be required here
            "/api/v1/users/forgot-password", // the user is locked out, so cannot have a token
            "/api/v1/users/reset-password", // the emailed single-use token in the body is the credential
            "/api/v1/market-data/**");

    // Service-to-service endpoints, never exposed to clients: wallet settlement called by order-service.
    private static final List<String> INTERNAL_PATHS = List.of(
            "/api/v1/users/*/funds/deduct",
            "/api/v1/users/*/funds/credit");

    // Back-office endpoints: only tokens with role ADMIN may call them through the gateway.
    private static final List<String> ADMIN_PATHS = List.of(
            "/api/v1/accounts/*/dividends",
            "/api/v1/accounts/*/interest");

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final SecretKey signingKey;
    private final String issuer;

    public JwtAuthFilter(@Value("${jwt.secret}") String jwtSecret,
                         @Value("${jwt.issuer}") String issuer) {
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
    }

    /**
     * Only routed API calls are authenticated. Actuator, CORS preflight and
     * public endpoints pass through untouched.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        String path = request.getRequestURI();

        return !path.startsWith("/api/")
                || HttpMethod.OPTIONS.matches(request.getMethod())
                || PUBLIC_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (INTERNAL_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path))) {
            log.warn("Blocked external call to internal endpoint: {} {}", request.getMethod(), path);
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("Missing or malformed Authorization header: {} {}", request.getMethod(), path);
            unauthorized(response, "Missing or invalid Authorization header");
            return;
        }

        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(issuer)
                    .build()
                    .parseSignedClaims(authHeader.substring(BEARER_PREFIX.length()))
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("JWT validation failed for {} {}: {}", request.getMethod(), path, e.getMessage());
            unauthorized(response, "Invalid or expired token");
            return;
        }

        if (!ACCESS_TOKEN_TYPE.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            log.warn("Non-access token used for {} {}", request.getMethod(), path);
            unauthorized(response, "Access token required");
            return;
        }

        String userId = claims.get(CLAIM_USER_ID, String.class);
        if (userId == null) {
            userId = claims.getSubject();
        }

        // Tokens issued before roles existed have no role claim: treat them as USER.
        String role = claims.get(CLAIM_ROLE, String.class);
        if (role == null || role.isBlank()) {
            role = DEFAULT_ROLE;
        }

        if (!ADMIN_ROLE.equals(role) && ADMIN_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path))) {
            log.warn("Non-admin user {} blocked from admin endpoint {} {}", userId, request.getMethod(), path);
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        Map<String, String> identity = new LinkedHashMap<>();
        identity.put(USER_ID_HEADER, userId);
        identity.put(USER_ROLE_HEADER, role);
        String email = claims.get(CLAIM_EMAIL, String.class);
        if (email != null) {
            identity.put(USER_EMAIL_HEADER, email);
        }

        log.debug("JWT validated. userId={}, role={}, path={}", userId, role, path);

        chain.doFilter(new IdentityHeadersRequest(request, identity), response);
    }

    private void unauthorized(HttpServletResponse response, String message) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}");
    }

    /**
     * Replaces any client-supplied X-User-* headers with the identity taken from the token.
     */
    static class IdentityHeadersRequest extends HttpServletRequestWrapper {

        private final Map<String, String> identity;

        IdentityHeadersRequest(HttpServletRequest request, Map<String, String> identity) {
            super(request);
            this.identity = identity;
        }

        @Override
        public String getHeader(String name) {
            String value = identityValue(name);
            if (value != null) {
                return value;
            }
            return isIdentityHeader(name) ? null : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String value = identityValue(name);
            if (value != null) {
                return Collections.enumeration(List.of(value));
            }
            return isIdentityHeader(name) ? Collections.emptyEnumeration() : super.getHeaders(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = new ArrayList<>(identity.keySet());
            for (String name : Collections.list(super.getHeaderNames())) {
                if (!isIdentityHeader(name)) {
                    names.add(name);
                }
            }
            return Collections.enumeration(names);
        }

        private String identityValue(String name) {
            return identity.entrySet().stream()
                    .filter(entry -> entry.getKey().equalsIgnoreCase(name))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
        }

        private static boolean isIdentityHeader(String name) {
            return name.toLowerCase().startsWith(IDENTITY_HEADER_PREFIX);
        }
    }
}
