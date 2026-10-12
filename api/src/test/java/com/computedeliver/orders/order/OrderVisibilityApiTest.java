package com.computedeliver.orders.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.computedeliver.orders.ApiTestSupport;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

class OrderVisibilityApiTest extends ApiTestSupport {

    private List<Long> idsOf(JsonNode array) {
        List<Long> ids = new ArrayList<>();
        array.forEach(n -> ids.add(n.get("id").asLong()));
        return ids;
    }

    @Test
    void clientSeesOnlyOwnOrders() {
        long a = placeOrder(ACME);
        long b = placeOrder(GLOBEX);
        List<Long> aList = idsOf(asClient(HttpMethod.GET, "/orders", null, ACME).getBody());
        assertThat(aList).contains(a).doesNotContain(b);
        List<Long> bList = idsOf(asClient(HttpMethod.GET, "/orders", null, GLOBEX).getBody());
        assertThat(bList).contains(b).doesNotContain(a);
    }

    @Test
    void operatorSeesAllOrders() {
        long a = placeOrder(ACME);
        long b = placeOrder(GLOBEX);
        assertThat(idsOf(asOperator(HttpMethod.GET, "/orders", null).getBody())).contains(a, b);
    }

    @Test
    void clientWithNoOrdersGetsEmptyListNotAnError() {
        jdbc.update("insert into clients (id, name, discount_bp) values (100, 'Empty Co', 500) on conflict do nothing");
        var res = asClient(HttpMethod.GET, "/orders", null, 100);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEmpty();
    }

    @Test
    void clientCannotOpenAnotherClientsOrder() {
        long b = placeOrder(GLOBEX);
        assertThat(asClient(HttpMethod.GET, "/orders/" + b, null, ACME).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void detailIncludesLinesPricingStatusAndHistory() {
        long id = placeOrder(ACME);
        JsonNode o = asClient(HttpMethod.GET, "/orders/" + id, null, ACME).getBody();
        assertThat(o.get("lines")).hasSize(1);
        assertThat(o.get("lines").get(0).get("name").asText()).isNotBlank();
        assertThat(o.get("grossCents").asLong()).isPositive();
        assertThat(o.get("status").asText()).isEqualTo("SUBMITTED");
        assertThat(o.get("history")).isNotEmpty();
    }
}
