package com.computedeliver.orders.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.computedeliver.orders.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

class RoleHeaderFilterTest extends PostgresTestBase {

    @Autowired
    TestRestTemplate rest;

    private HttpStatus get(String path, String role, String clientId) {
        HttpHeaders headers = new HttpHeaders();
        if (role != null) {
            headers.add("X-Role", role);
        }
        if (clientId != null) {
            headers.add("X-Client-Id", clientId);
        }
        return HttpStatus.valueOf(
                rest.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class).getStatusCode().value());
    }

    @Test
    void missingRoleIsRejected() {
        assertThat(get("/clients", null, null)).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void unknownRoleIsRejected() {
        assertThat(get("/clients", "SUPER_ADMIN", null)).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void invoiceDepartmentIsForbidden() {
        assertThat(get("/clients", "INVOICE_DEPARTMENT", null)).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get("/orders", "INVOICE_DEPARTMENT", null)).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void enterpriseClientNeedsClientIdOnOrders() {
        assertThat(get("/orders", "ENTERPRISE_CLIENT", null)).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void invalidOrUnknownClientIdIsRejected() {
        assertThat(get("/orders", "ENTERPRISE_CLIENT", "abc")).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(get("/orders", "ENTERPRISE_CLIENT", "9999")).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void enterpriseClientCanListClientsWithoutPickingOne() {
        assertThat(get("/clients", "ENTERPRISE_CLIENT", null)).isEqualTo(HttpStatus.OK);
    }

    @Test
    void warehouseOperatorNeedsNoClientId() {
        assertThat(get("/products", "WAREHOUSE_OPERATOR", null)).isEqualTo(HttpStatus.OK);
    }

    @Test
    void actuatorAndDocsAreNotGated() {
        assertThat(get("/actuator/health", null, null)).isEqualTo(HttpStatus.OK);
        assertThat(get("/v3/api-docs", null, null)).isEqualTo(HttpStatus.OK);
    }
}
