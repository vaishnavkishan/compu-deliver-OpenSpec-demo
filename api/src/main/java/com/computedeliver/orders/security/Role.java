package com.computedeliver.orders.security;

import java.util.Optional;

public enum Role {
    ENTERPRISE_CLIENT(true),
    WAREHOUSE_OPERATOR(true),
    /** Listed in the portal as a future role; the API rejects it. */
    INVOICE_DEPARTMENT(false);

    private final boolean functional;

    Role(boolean functional) {
        this.functional = functional;
    }

    public boolean isFunctional() {
        return functional;
    }

    public static Optional<Role> parse(String value) {
        if (value == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(valueOf(value.trim().toUpperCase()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
