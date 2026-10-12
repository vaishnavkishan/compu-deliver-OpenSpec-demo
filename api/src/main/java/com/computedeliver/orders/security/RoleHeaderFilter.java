package com.computedeliver.orders.security;

import com.computedeliver.orders.reference.ReferenceData;
import com.computedeliver.orders.web.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the self-declared {@code X-Role} and {@code X-Client-Id} headers on API calls. This is a demo
 * convenience, not authentication: anyone can send any role.
 */
@Component
public class RoleHeaderFilter extends OncePerRequestFilter {

    private final ReferenceData referenceData;
    private final ObjectMapper objectMapper;

    RoleHeaderFilter(ReferenceData referenceData, ObjectMapper objectMapper) {
        this.referenceData = referenceData;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return request.getMethod().equals("OPTIONS")
                || !(path.equals("/clients") || path.equals("/products") || path.startsWith("/orders"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String roleHeader = request.getHeader("X-Role");
        if (roleHeader == null || roleHeader.isBlank()) {
            reject(response, HttpStatus.BAD_REQUEST, "X-Role header is required");
            return;
        }
        Role role = Role.parse(roleHeader).orElse(null);
        if (role == null) {
            reject(response, HttpStatus.BAD_REQUEST, "Unknown role: " + roleHeader);
            return;
        }
        if (!role.isFunctional()) {
            reject(response, HttpStatus.FORBIDDEN, "Role " + role + " is not available yet");
            return;
        }

        Long clientId = null;
        String clientHeader = request.getHeader("X-Client-Id");
        if (role == Role.ENTERPRISE_CLIENT) {
            // The client list itself is needed to pick a client, so it is exempt from requiring one.
            boolean needsClient = request.getRequestURI().startsWith("/orders");
            if (clientHeader == null || clientHeader.isBlank()) {
                if (needsClient) {
                    reject(response, HttpStatus.BAD_REQUEST, "X-Client-Id header is required for ENTERPRISE_CLIENT");
                    return;
                }
            } else {
                try {
                    clientId = Long.parseLong(clientHeader.trim());
                } catch (NumberFormatException e) {
                    reject(response, HttpStatus.BAD_REQUEST, "X-Client-Id must be a number");
                    return;
                }
                if (referenceData.findClient(clientId).isEmpty()) {
                    reject(response, HttpStatus.BAD_REQUEST, "Unknown client: " + clientId);
                    return;
                }
            }
        }

        request.setAttribute(Caller.ATTRIBUTE, new Caller(role, clientId));
        chain.doFilter(request, response);
    }

    private void reject(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiError.of(status, message));
    }
}
