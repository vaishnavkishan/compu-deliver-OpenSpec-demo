package com.computedeliver.orders.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.computedeliver.orders.ApiTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SuppressWarnings("unchecked")
class OrderIntakeApiTest extends ApiTestSupport {

    private ResponseEntity<JsonNode> place(long clientId, Map<String, Object> body) {
        return asClient(HttpMethod.POST, "/orders", body, clientId);
    }

    @Test
    void acceptedOrderIsSubmittedWithContractPricing() {
        // 10 x 429.50 = 4295.00 gross, 10% -> 429.50 discount
        ResponseEntity<JsonNode> res = place(ACME, orderOf(line("MON-27-4K", 10)));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode o = res.getBody();
        assertThat(o.get("status").asText()).isEqualTo("SUBMITTED");
        assertThat(o.get("clientId").asLong()).isEqualTo(ACME);
        assertThat(o.get("grossCents").asLong()).isEqualTo(429_500);
        assertThat(o.get("discountBp").asInt()).isEqualTo(1000);
        assertThat(o.get("netCents").asLong()).isEqualTo(386_550);
        assertThat(o.get("totalUnits").asInt()).isEqualTo(10);
    }

    @Test
    void multipleLinesAreSummed() {
        JsonNode o = place(GLOBEX, orderOf(line("MON-27-4K", 2), line("MS-WL-PRO", 3))).getBody();
        long gross = 2 * 42_950L + 3 * 5_499L;
        assertThat(o.get("grossCents").asLong()).isEqualTo(gross);
        assertThat(o.get("netCents").asLong()).isEqualTo(gross - Pricing.discountCents(gross, 1500));
    }

    @Test
    void zeroDiscountClientPaysGross() {
        JsonNode o = place(INITECH, orderOf(line("KB-WL-ERGO", 4))).getBody();
        assertThat(o.get("discountBp").asInt()).isZero();
        assertThat(o.get("netCents").asLong()).isEqualTo(o.get("grossCents").asLong());
    }

    @Test
    void clientWithoutContractIsRejected() {
        ResponseEntity<JsonNode> res = place(UMBRELLA, orderOf(line("MS-WL-PRO", 1)));
        assertThat(res.getStatusCode().is4xxClientError()).isTrue();
        assertThat(res.getBody().get("message").asText()).containsIgnoringCase("no contract");
        assertThat(jdbc.queryForObject("select count(*) from orders where client_id = 4", Integer.class)).isZero();
    }

    @Test
    void emptyOrderIsRejected() {
        int before = jdbc.queryForObject("select count(*) from orders", Integer.class);
        assertThat(place(ACME, Map.of("lines", List.of())).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(asClient(HttpMethod.POST, "/orders", null, ACME).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(jdbc.queryForObject("select count(*) from orders", Integer.class)).isEqualTo(before);
    }

    @Test
    void invalidQuantitiesIdentifyTheLine() {
        for (Object bad : List.of(0, -3, 1.5)) {
            ResponseEntity<JsonNode> res = place(ACME, orderOf(line("MS-WL-PRO", 1), line("MS-WL-PRO", bad)));
            assertThat(res.getStatusCode()).as("quantity %s", bad).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(res.getBody().get("message").asText()).contains("Line 2");
        }
    }

    @Test
    void unknownSkuIsRejectedAndNamed() {
        ResponseEntity<JsonNode> res = place(ACME, orderOf(line("NOPE-1", 1)));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().get("message").asText()).contains("NOPE-1");
    }

    @Test
    void warehouseOperatorCannotPlaceOrders() {
        ResponseEntity<JsonNode> res = asOperator(HttpMethod.POST, "/orders", orderOf(line("MS-WL-PRO", 1)));
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void contractOrPriceChangeAfterIntakeDoesNotAlterExistingOrder() {
        JsonNode placed = place(ACME, orderOf(line("SW-POE-24", 5))).getBody();
        long id = placed.get("id").asLong();
        try {
            jdbc.update("update clients set discount_bp = 2500 where id = ?", ACME);
            jdbc.update("update products set unit_price_cents = 1 where sku = 'SW-POE-24'");

            JsonNode again = asClient(HttpMethod.GET, "/orders/" + id, null, ACME).getBody();
            assertThat(again.get("grossCents").asLong()).isEqualTo(placed.get("grossCents").asLong());
            assertThat(again.get("discountBp").asInt()).isEqualTo(1000);
            assertThat(again.get("netCents").asLong()).isEqualTo(placed.get("netCents").asLong());
            assertThat(again.get("lines").get(0).get("unitPriceCents").asLong()).isEqualTo(38_950);
        } finally {
            jdbc.update("update clients set discount_bp = 1000 where id = ?", ACME);
            jdbc.update("update products set unit_price_cents = 38950 where sku = 'SW-POE-24'");
        }
    }
}
