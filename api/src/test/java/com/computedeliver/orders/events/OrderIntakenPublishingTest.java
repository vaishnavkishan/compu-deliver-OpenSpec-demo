package com.computedeliver.orders.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.computedeliver.orders.ApiTestSupport;
import com.computedeliver.orders.order.OrderIntakenEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionTemplate;

class OrderIntakenPublishingTest extends ApiTestSupport {

    @Autowired
    RabbitTemplate rabbit;
    @Autowired
    AmqpAdmin admin;
    @Autowired
    ApplicationEventPublisher events;
    @Autowired
    TransactionTemplate tx;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void drain() {
        admin.purgeQueue(RabbitConfig.VERIFICATION_QUEUE, true);
    }

    private JsonNode receive(long timeoutMs) throws Exception {
        Message m = rabbit.receive(RabbitConfig.VERIFICATION_QUEUE, timeoutMs);
        return m == null ? null : mapper.readTree(m.getBody());
    }

    @Test
    void oneMessageIsPublishedForAnAcceptedOrder() throws Exception {
        var res = asClient(HttpMethod.POST, "/orders", orderOf(line("MON-27-4K", 2), line("MS-WL-PRO", 1)), ACME);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        JsonNode order = res.getBody();

        JsonNode msg = receive(5_000);
        assertThat(msg).isNotNull();
        assertThat(msg.get("orderId").asLong()).isEqualTo(order.get("id").asLong());
        assertThat(msg.get("clientId").asLong()).isEqualTo(ACME);
        assertThat(msg.get("grossCents").asLong()).isEqualTo(order.get("grossCents").asLong());
        assertThat(msg.get("discountBp").asInt()).isEqualTo(1000);
        assertThat(msg.get("netCents").asLong()).isEqualTo(order.get("netCents").asLong());
        assertThat(msg.get("timestamp").asText()).isNotBlank();
        assertThat(msg.get("lines")).hasSize(2);
        assertThat(msg.get("lines").get(0).get("sku").asText()).isEqualTo("MON-27-4K");
        assertThat(msg.get("lines").get(0).get("quantity").asInt()).isEqualTo(2);

        assertThat(receive(500)).as("exactly one message").isNull();
    }

    @Test
    void nothingIsPublishedForRejectedOrders() throws Exception {
        asClient(HttpMethod.POST, "/orders", orderOf(line("NOPE", 1)), ACME);              // unknown SKU
        asClient(HttpMethod.POST, "/orders", orderOf(line("MS-WL-PRO", 0)), ACME);         // bad quantity
        asClient(HttpMethod.POST, "/orders", orderOf(line("MS-WL-PRO", 1)), UMBRELLA);     // no contract
        assertThat(receive(1_000)).isNull();
    }

    @Test
    void nothingIsPublishedWhenTheTransactionRollsBack() throws Exception {
        tx.executeWithoutResult(status -> {
            events.publishEvent(new OrderIntakenEvent(1, ACME, List.of(), 0, 0, 0, Instant.now()));
            status.setRollbackOnly();
        });
        assertThat(receive(1_000)).isNull();
    }

    @Test
    void messageIsPublishedWhenAnEventTransactionCommits() throws Exception {
        tx.executeWithoutResult(status ->
                events.publishEvent(new OrderIntakenEvent(42, ACME, List.of(), 0, 0, 0, Instant.now())));
        JsonNode msg = receive(5_000);
        assertThat(msg).isNotNull();
        assertThat(msg.get("orderId").asLong()).isEqualTo(42);
    }
}
