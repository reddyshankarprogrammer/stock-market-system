package com.stockmarket.apigateway.filter;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthFilterTest {

    private static final String SECRET = "test-secret-key-that-is-long-enough-for-hs256";
    private static final String ISSUER = "stock-market-system";

    private final JwtAuthFilter filter = new JwtAuthFilter(SECRET, ISSUER);

    @Test
    void validAccessToken_forwardsIdentityAndStripsSpoofedHeaders() throws Exception {
        MockHttpServletRequest request = get("/api/v1/orders/my-orders");
        request.addHeader("Authorization", "Bearer " + token("ACCESS", SECRET, ISSUER, 60));
        request.addHeader("X-User-Id", "attacker");
        request.addHeader("X-User-Role", "ADMIN");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        HttpServletRequest forwarded = (HttpServletRequest) chain.getRequest();
        assertThat(forwarded).isNotNull();
        assertThat(forwarded.getHeader("X-User-Id")).isEqualTo("user-1");
        assertThat(forwarded.getHeader("x-user-email")).isEqualTo("user@example.com");
        assertThat(forwarded.getHeader("X-User-Role")).isEqualTo("USER");
        assertThat(Collections.list(forwarded.getHeaders("X-User-Id"))).containsExactly("user-1");
        assertThat(Collections.list(forwarded.getHeaderNames()))
                .filteredOn(name -> name.equalsIgnoreCase("X-User-Id")).hasSize(1);
    }

    @Test
    void missingToken_returns401() throws Exception {
        assertThat(run(get("/api/v1/orders")).getStatus()).isEqualTo(401);
    }

    @Test
    void refreshToken_returns401() throws Exception {
        MockHttpServletRequest request = get("/api/v1/orders");
        request.addHeader("Authorization", "Bearer " + token("REFRESH", SECRET, ISSUER, 60));

        assertThat(run(request).getStatus()).isEqualTo(401);
    }

    @Test
    void expiredOrWronglySignedToken_returns401() throws Exception {
        MockHttpServletRequest expired = get("/api/v1/orders");
        expired.addHeader("Authorization", "Bearer " + token("ACCESS", SECRET, ISSUER, -60));
        assertThat(run(expired).getStatus()).isEqualTo(401);

        MockHttpServletRequest forged = get("/api/v1/orders");
        forged.addHeader("Authorization",
                "Bearer " + token("ACCESS", "another-secret-key-that-is-long-enough-too", ISSUER, 60));
        assertThat(run(forged).getStatus()).isEqualTo(401);

        MockHttpServletRequest wrongIssuer = get("/api/v1/orders");
        wrongIssuer.addHeader("Authorization", "Bearer " + token("ACCESS", SECRET, "someone-else", 60));
        assertThat(run(wrongIssuer).getStatus()).isEqualTo(401);
    }

    @Test
    void publicAndNonApiPaths_passWithoutToken() throws Exception {
        MockHttpServletRequest login = new MockHttpServletRequest("POST", "/api/v1/users/login");
        assertThat(run(login).getStatus()).isEqualTo(200);
        MockHttpServletRequest refresh = new MockHttpServletRequest("POST", "/api/v1/users/refresh");
        assertThat(run(refresh).getStatus()).isEqualTo(200);
        assertThat(run(get("/api/v1/market-data/stocks/AAPL")).getStatus()).isEqualTo(200);
        assertThat(run(get("/actuator/health")).getStatus()).isEqualTo(200);
    }

    @Test
    void refreshEndpointIsPublic_butRefreshTokensStillCannotCallProtectedPaths() throws Exception {
        // Public: no Authorization header needed (the access token has expired by then). Like login and register,
        // public paths skip the filter, so the user-service refresh handler must never read X-User-* (it does not).
        MockHttpServletRequest refresh = new MockHttpServletRequest("POST", "/api/v1/users/refresh");
        assertThat(run(refresh).getStatus()).isEqualTo(200);

        // The rule that makes the endpoint necessary stays: a refresh token as a Bearer token is rejected elsewhere.
        MockHttpServletRequest protectedPath = get("/api/v1/users/me");
        protectedPath.addHeader("Authorization", "Bearer " + token("REFRESH", SECRET, ISSUER, 60));
        assertThat(run(protectedPath).getStatus()).isEqualTo(401);
    }

    @Test
    void internalFundsEndpoints_areBlockedEvenWithValidToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/users/user-2/funds/deduct");
        request.addHeader("Authorization", "Bearer " + token("ACCESS", SECRET, ISSUER, 60));

        assertThat(run(request).getStatus()).isEqualTo(403);
    }

    @Test
    void adminEndpoints_requireAdminRole() throws Exception {
        for (String path : new String[]{"/api/v1/accounts/acc-1/dividends", "/api/v1/accounts/acc-1/interest"}) {
            MockHttpServletRequest asUser = new MockHttpServletRequest("POST", path);
            asUser.addHeader("Authorization", "Bearer " + token("ACCESS", SECRET, ISSUER, 60));
            assertThat(run(asUser).getStatus()).isEqualTo(403);

            MockHttpServletRequest asAdmin = new MockHttpServletRequest("POST", path);
            asAdmin.addHeader("Authorization", "Bearer " + token("ACCESS", SECRET, ISSUER, 60, "ADMIN"));
            MockFilterChain chain = new MockFilterChain();
            filter.doFilter(asAdmin, new MockHttpServletResponse(), chain);
            assertThat(((HttpServletRequest) chain.getRequest()).getHeader("X-User-Role")).isEqualTo("ADMIN");
        }
    }

    @Test
    void roleIsForwardedAndDefaultsToUser() throws Exception {
        MockHttpServletRequest request = get("/api/v1/orders");
        request.addHeader("Authorization", "Bearer " + token("ACCESS", SECRET, ISSUER, 60));
        request.addHeader("X-User-Role", "ADMIN");
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        assertThat(((HttpServletRequest) chain.getRequest()).getHeader("X-User-Role")).isEqualTo("USER");
    }

    private MockHttpServletResponse run(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private MockHttpServletRequest get(String path) {
        return new MockHttpServletRequest("GET", path);
    }

    private String token(String tokenType, String secret, String issuer, long expiresInSeconds) {
        return token(tokenType, secret, issuer, expiresInSeconds, null);
    }

    private String token(String tokenType, String secret, String issuer, long expiresInSeconds, String role) {
        Instant now = Instant.now();
        Map<String, Object> claims = new java.util.HashMap<>(
                Map.of("userId", "user-1", "email", "user@example.com", "tokenType", tokenType));
        if (role != null) {
            claims.put("role", role);
        }
        return Jwts.builder()
                .claims(claims)
                .subject("user-1")
                .issuer(issuer)
                .issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(now.plusSeconds(expiresInSeconds)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)), Jwts.SIG.HS256)
                .compact();
    }
}
