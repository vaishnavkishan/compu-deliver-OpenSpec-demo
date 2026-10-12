package com.computedeliver.orders;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

public abstract class ApiTestSupport extends PostgresTestBase {

    public static final long ACME = 1;       // 10%
    public static final long GLOBEX = 2;     // 15%
    public static final long INITECH = 3;    // 0%
    public static final long UMBRELLA = 4;   // no contract

    @Autowired
    protected TestRestTemplate rest;
    @Autowired
    protected JdbcTemplate jdbc;

    protected ResponseEntity<JsonNode> call(HttpMethod method, String path, Object body, String role, Long clientId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (role != null) {
            headers.add("X-Role", role);
        }
        if (clientId != null) {
            headers.add("X-Client-Id", String.valueOf(clientId));
        }
        return rest.exchange(path, method, new HttpEntity<>(body, headers), JsonNode.class);
    }

    protected ResponseEntity<JsonNode> asClient(HttpMethod method, String path, Object body, long clientId) {
        return call(method, path, body, "ENTERPRISE_CLIENT", clientId);
    }

    protected ResponseEntity<JsonNode> asOperator(HttpMethod method, String path, Object body) {
        return call(method, path, body, "WAREHOUSE_OPERATOR", null);
    }

    protected static Map<String, Object> line(String sku, Object quantity) {
        return Map.of("sku", sku, "quantity", quantity);
    }

    protected static Map<String, Object> orderOf(Map<String, Object>... lines) {
        return Map.of("lines", List.of(lines));
    }

    @SuppressWarnings("unchecked")
    protected long placeOrder(long clientId) {
        ResponseEntity<JsonNode> res = asClient(HttpMethod.POST, "/orders", orderOf(line("MS-WL-PRO", 2)), clientId);
        return res.getBody().get("id").asLong();
    }

    protected JsonNode advance(long orderId) {
        return asOperator(HttpMethod.POST, "/orders/" + orderId + "/advance", null).getBody();
    }

    protected long orderAt(long clientId, String status) {
        long id = placeOrder(clientId);
        while (!asOperator(HttpMethod.GET, "/orders/" + id, null).getBody().get("status").asText().equals(status)) {
            advance(id);
        }
        return id;
    }
}
