package com.computedeliver.orders.security;

import com.computedeliver.orders.web.ApiException;
import org.springframework.http.HttpStatus;

/** The self-declared role and client of the current request. Not a security boundary. */
public record Caller(Role role, Long clientId) {

    public static final String ATTRIBUTE = "com.computedeliver.orders.security.Caller";

    public boolean isClient() {
        return role == Role.ENTERPRISE_CLIENT;
    }

    public boolean isOperator() {
        return role == Role.WAREHOUSE_OPERATOR;
    }

    public void requireRole(Role required) {
        if (role != required) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This action requires the " + required + " role");
        }
    }

    /** The acting client id; only valid for Enterprise Client callers on endpoints that require one. */
    public long requireClientId() {
        if (clientId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "X-Client-Id header is required");
        }
        return clientId;
    }
}
