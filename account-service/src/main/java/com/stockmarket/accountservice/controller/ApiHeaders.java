package com.stockmarket.accountservice.controller;

/**
 * Request headers set by the api-gateway.
 */
public final class ApiHeaders {

    /**
     * Id of the authenticated user, taken by the api-gateway from the validated JWT.
     * Clients cannot set it: the gateway strips any client-sent X-User-* header.
     */
    public static final String USER_ID = "X-User-Id";

    public static final String USER_ID_DESCRIPTION =
            "Authenticated user id. Set by the api-gateway from the JWT; set it yourself only when calling the service directly.";

    private ApiHeaders() {
    }
}
