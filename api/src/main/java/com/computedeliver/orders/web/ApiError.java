package com.computedeliver.orders.web;

import java.time.Instant;

public record ApiError(int status, String error, String message, Instant timestamp) {

    public static ApiError of(org.springframework.http.HttpStatus status, String message) {
        return new ApiError(status.value(), status.getReasonPhrase(), message, Instant.now());
    }
}
