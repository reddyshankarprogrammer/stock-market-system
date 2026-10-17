package com.stockmarket.accountservice.security;

import com.stockmarket.accountservice.exception.AccountAccessDeniedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

/**
 * Identity of the caller of the current HTTP request.
 *
 * The api-gateway validates the JWT and sets {@code X-User-Id} and {@code X-User-Role};
 * it strips any X-User-* header the client sent, so these values can be trusted as long
 * as the service is only reachable through the gateway (see docs/account-service-security.md).
 */
public final class RequestUser {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String USER_ROLE_HEADER = "X-User-Role";
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String SYSTEM = "system";

    private RequestUser() {
    }

    /**
     * The caller's user id, or {@value #SYSTEM} outside a request or for internal calls.
     */
    public static String userId() {
        return header(USER_ID_HEADER).orElse(SYSTEM);
    }

    /**
     * The caller's role; USER when the header is missing.
     */
    public static String role() {
        return header(USER_ROLE_HEADER).orElse(ROLE_USER);
    }

    public static boolean isAdmin() {
        return ROLE_ADMIN.equals(role());
    }

    /**
     * @throws AccountAccessDeniedException unless the caller is an ADMIN
     */
    public static void requireAdmin() {
        if (!isAdmin()) {
            throw AccountAccessDeniedException.adminOnly();
        }
    }

    /**
     * The value of a header of the current request, if there is a request and the header is set.
     */
    public static Optional<String> header(String name) {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            HttpServletRequest request = attributes.getRequest();
            String value = request.getHeader(name);
            if (value != null && !value.isBlank()) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }
}
