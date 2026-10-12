package com.computedeliver.orders.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.computedeliver.orders.ApiTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class OrderEditApiTest extends ApiTestSupport {

    private ResponseEntity<JsonNode> edit(long id, Object body, long clientId) {
        return asClient(HttpMethod.PUT, "/orders/" + id, body, clientId);
    }

    private JsonNode get(long id) {
        return asOperator(HttpMethod.GET, "/orders/" + id, null).getBody();
    }

    @Test
    void submittedOrderLinesAreReplacedAndStatusStaysSubmitted() {
        long id = placeOrder(ACME);
        ResponseEntity<JsonNode> res = edit(id, orderOf(line("MON-27-4K", 3)), ACME);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = res.getBody();
        assertThat(body.get("status").asText()).isEqualTo("SUBMITTED");
        assertThat(body.get("lines")).hasSize(1);
        assertThat(body.get("lines").get(0).get("sku").asText()).isEqualTo("MON-27-4K");
        assertThat(body.get("lines").get(0).get("quantity").asInt()).isEqualTo(3);
        assertThat(get(id).get("lines")).hasSize(1);
        assertThat(get(id).get("history")).hasSize(1);
    }

    @Test
    void totalsAreRecomputedFromTheCurrentContractAndPrices() {
        long id = placeOrder(ACME);
        long unit = jdbc.queryForObject("select unit_price_cents from products where sku = 'MS-WL-PRO'", Long.class);
        JsonNode body = edit(id, orderOf(line("MS-WL-PRO", 5)), ACME).getBody();
        long gross = unit * 5;
        assertThat(body.get("grossCents").asLong()).isEqualTo(gross);
        assertThat(body.get("discountBp").asInt()).isEqualTo(1000);
        assertThat(body.get("netCents").asLong()).isEqualTo(gross - Math.round(gross * 0.10));
    }

    @Test
    void editUsesCurrentContractDiscountNotTheOldSnapshot() {
        long id = placeOrder(INITECH);
        assertThat(get(id).get("discountBp").asInt()).isZero();
        jdbc.update("update clients set discount_bp = 2000 where id = ?", INITECH);
        try {
            assertThat(edit(id, orderOf(line("MS-WL-PRO", 1)), INITECH).getBody().get("discountBp").asInt())
                    .isEqualTo(2000);
        } finally {
            jdbc.update("update clients set discount_bp = 0 where id = ?", INITECH);
        }
    }

    @Test
    void editTimestampIsRecorded() {
        long id = placeOrder(ACME);
        assertThat(get(id).get("edits")).isEmpty();
        JsonNode edits = edit(id, orderOf(line("MS-WL-PRO", 1)), ACME).getBody().get("edits");
        assertThat(edits).hasSize(1);
        assertThat(edits.get(0).get("editedAt").asText()).isNotBlank();
        assertThat(get(id).get("edits")).hasSize(1);
    }

    @Test
    void editAfterTheIntakeStepIsRejectedAndOrderUnchanged() {
        for (String status : new String[] {"CONFIRMED", "PROCESSING", "SHIPPED", "DELIVERED"}) {
            long id = orderAt(ACME, status);
            assertThat(edit(id, orderOf(line("MON-27-4K", 9)), ACME).getStatusCode()).as(status)
                    .isEqualTo(HttpStatus.CONFLICT);
            assertThat(get(id).get("lines").get(0).get("sku").asText()).isEqualTo("MS-WL-PRO");
            assertThat(get(id).get("edits")).isEmpty();
        }
        long cancelled = placeOrder(ACME);
        asClient(HttpMethod.POST, "/orders/" + cancelled + "/cancel", null, ACME);
        assertThat(edit(cancelled, orderOf(line("MON-27-4K", 9)), ACME).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void anotherClientsOrderCannotBeEdited() {
        long id = placeOrder(GLOBEX);
        assertThat(edit(id, orderOf(line("MON-27-4K", 9)), ACME).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get(id).get("lines").get(0).get("sku").asText()).isEqualTo("MS-WL-PRO");
    }

    @Test
    void warehouseOperatorCannotEdit() {
        long id = placeOrder(ACME);
        assertThat(asOperator(HttpMethod.PUT, "/orders/" + id, orderOf(line("MON-27-4K", 1))).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(get(id).get("edits")).isEmpty();
    }

    @Test
    void invalidEditsAreRejectedAndOrderUnchanged() {
        long id = placeOrder(ACME);
        JsonNode before = get(id);
        assertThat(edit(id, orderOf(), ACME).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(edit(id, orderOf(line("MS-WL-PRO", 0)), ACME).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(edit(id, orderOf(line("MS-WL-PRO", 1.5)), ACME).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(edit(id, orderOf(line("MS-WL-PRO", 1), line("NOPE", 1)), ACME).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(get(id)).isEqualTo(before);
    }

    @Test
    void editWhenTheClientNoLongerHasAContractIsRejected() {
        long id = placeOrder(INITECH);
        jdbc.update("update clients set discount_bp = null where id = ?", INITECH);
        try {
            assertThat(edit(id, orderOf(line("MS-WL-PRO", 1)), INITECH).getStatusCode())
                    .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(get(id).get("edits")).isEmpty();
        } finally {
            jdbc.update("update clients set discount_bp = 0 where id = ?", INITECH);
        }
    }

    @Test
    void advancingToConfirmedMakesLaterEditsFail() {
        long id = placeOrder(ACME);
        assertThat(edit(id, orderOf(line("MS-WL-PRO", 4)), ACME).getStatusCode()).isEqualTo(HttpStatus.OK);
        advance(id);
        assertThat(edit(id, orderOf(line("MS-WL-PRO", 5)), ACME).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(get(id).get("lines").get(0).get("quantity").asInt()).isEqualTo(4);
    }
}
