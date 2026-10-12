package com.computedeliver.orders.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.computedeliver.orders.ApiTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class OrderCancelApiTest extends ApiTestSupport {

    private ResponseEntity<JsonNode> cancel(long id, long clientId) {
        return asClient(HttpMethod.POST, "/orders/" + id + "/cancel", null, clientId);
    }

    private String statusOf(long id) {
        return asOperator(HttpMethod.GET, "/orders/" + id, null).getBody().get("status").asText();
    }

    @Test
    void submittedOrderCanBeCancelledAndIsRecorded() {
        long id = placeOrder(ACME);
        ResponseEntity<JsonNode> res = cancel(id, ACME);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().get("status").asText()).isEqualTo("CANCELLED");
        JsonNode history = res.getBody().get("history");
        assertThat(history.get(history.size() - 1).get("status").asText()).isEqualTo("CANCELLED");
    }

    @Test
    void confirmedOrderCanBeCancelled() {
        long id = orderAt(ACME, "CONFIRMED");
        assertThat(cancel(id, ACME).getBody().get("status").asText()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelAfterProcessingBeginsIsRejectedAndStatusUnchanged() {
        for (String status : new String[] {"PROCESSING", "SHIPPED", "DELIVERED"}) {
            long id = orderAt(ACME, status);
            assertThat(cancel(id, ACME).getStatusCode()).as(status).isEqualTo(HttpStatus.CONFLICT);
            assertThat(statusOf(id)).isEqualTo(status);
        }
    }

    @Test
    void cancellingTwiceIsRejected() {
        long id = placeOrder(ACME);
        cancel(id, ACME);
        assertThat(cancel(id, ACME).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(statusOf(id)).isEqualTo("CANCELLED");
    }

    @Test
    void anotherClientsOrderCannotBeCancelled() {
        long id = placeOrder(GLOBEX);
        assertThat(cancel(id, ACME).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(statusOf(id)).isEqualTo("SUBMITTED");
    }

    @Test
    void warehouseOperatorCannotCancel() {
        long id = placeOrder(ACME);
        ResponseEntity<JsonNode> res = asOperator(HttpMethod.POST, "/orders/" + id + "/cancel", null);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(statusOf(id)).isEqualTo("SUBMITTED");
    }
}
