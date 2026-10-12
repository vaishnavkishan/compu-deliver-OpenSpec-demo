package com.computedeliver.orders.reference;

import static org.assertj.core.api.Assertions.assertThat;

import com.computedeliver.orders.PostgresTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

class ReferenceDataTest extends PostgresTestBase {

    @Autowired
    ReferenceData referenceData;
    @Autowired
    TestRestTemplate rest;

    @org.junit.jupiter.api.BeforeEach
    void actAsOperator() {
        rest.getRestTemplate().getInterceptors().clear();
        rest.getRestTemplate().getInterceptors().add((req, body, exec) -> {
            req.getHeaders().set("X-Role", "WAREHOUSE_OPERATOR");
            return exec.execute(req, body);
        });
    }

    @Test
    void seedIncludesDiscountedZeroDiscountAndNoContractClients() {
        assertThat(referenceData.listClients()).extracting(Client::getDiscountBp)
                .contains(1000, 1500, 0, null);
    }

    @Test
    void clientWithoutContractHasNoContract() {
        Client noContract = referenceData.listClients().stream()
                .filter(c -> c.getDiscountBp() == null).findFirst().orElseThrow();
        assertThat(noContract.hasContract()).isFalse();
        Client zero = referenceData.listClients().stream()
                .filter(c -> Integer.valueOf(0).equals(c.getDiscountBp())).findFirst().orElseThrow();
        assertThat(zero.hasContract()).isTrue();
    }

    @Test
    void clientsEndpointListsSeededClients() {
        var clients = rest.getForObject("/clients", ReferenceController.ClientView[].class);
        assertThat(clients).extracting(ReferenceController.ClientView::name)
                .contains("Acme Corporation", "Globex Industries", "Initech Ltd", "Umbrella Logistics");
        assertThat(clients).filteredOn(c -> c.name().equals("Umbrella Logistics"))
                .singleElement().satisfies(c -> assertThat(c.hasContract()).isFalse());
    }

    @Test
    void productsEndpointListsSeededProducts() {
        var products = rest.getForObject("/products", ReferenceController.ProductView[].class);
        assertThat(products).isNotEmpty();
        assertThat(products).allMatch(p -> p.unitPriceCents() > 0);
    }
}
