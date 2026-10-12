package com.computedeliver.orders.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.computedeliver.orders.ApiTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class OrderLifecycleApiTest extends ApiTestSupport {

    private String statusOf(long id) {
        return asOperator(HttpMethod.GET, "/orders/" + id, null).getBody().get("status").asText();
    }

    @Test
    void operatorAdvancesOneStepAtATimeToDelivered() {
        long id = placeOrder(ACME);
        for (String expected : new String[] {"CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED"}) {
            ResponseEntity<JsonNode> res = asOperator(HttpMethod.POST, "/orders/" + id + "/advance", null);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(res.getBody().get("status").asText()).isEqualTo(expected);
        }
    }

    @Test
    void everyTransitionGetsAHistoryRowWithATimestamp() {
        long id = placeOrder(ACME);
        advance(id);
        JsonNode history = advance(id).get("history");
        assertThat(history).hasSize(3);
        assertThat(history.get(0).get("status").asText()).isEqualTo("SUBMITTED");
        assertThat(history.get(1).get("status").asText()).isEqualTo("CONFIRMED");
        assertThat(history.get(2).get("status").asText()).isEqualTo("PROCESSING");
        Instant previous = Instant.EPOCH;
        for (JsonNode h : history) {
            Instant at = Instant.parse(h.get("changedAt").asText());
            assertThat(at).isAfterOrEqualTo(previous);
            previous = at;
        }
    }

    @Test
    void newOrderHasInitialSubmittedHistoryEntry() {
        long id = placeOrder(GLOBEX);
        JsonNode order = asClient(HttpMethod.GET, "/orders/" + id, null, GLOBEX).getBody();
        assertThat(order.get("history")).hasSize(1);
        assertThat(order.get("history").get(0).get("status").asText()).isEqualTo("SUBMITTED");
        assertThat(order.get("history").get(0).get("changedAt").asText()).isNotBlank();
    }

    @Test
    void deliveredOrderCannotBeAdvanced() {
        long id = orderAt(ACME, "DELIVERED");
        ResponseEntity<JsonNode> res = asOperator(HttpMethod.POST, "/orders/" + id + "/advance", null);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(statusOf(id)).isEqualTo("DELIVERED");
    }

    @Test
    void cancelledOrderCannotBeAdvanced() {
        long id = placeOrder(ACME);
        asClient(HttpMethod.POST, "/orders/" + id + "/cancel", null, ACME);
        ResponseEntity<JsonNode> res = asOperator(HttpMethod.POST, "/orders/" + id + "/advance", null);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(statusOf(id)).isEqualTo("CANCELLED");
    }

    @Test
    void enterpriseClientCannotAdvance() {
        long id = placeOrder(ACME);
        ResponseEntity<JsonNode> res = asClient(HttpMethod.POST, "/orders/" + id + "/advance", null, ACME);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(statusOf(id)).isEqualTo("SUBMITTED");
    }

    @Test
    void advancingUnknownOrderIs404() {
        assertThat(asOperator(HttpMethod.POST, "/orders/999999/advance", null).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }
}
