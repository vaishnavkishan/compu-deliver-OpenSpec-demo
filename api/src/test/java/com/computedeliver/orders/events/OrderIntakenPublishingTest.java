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
        admin.purgeQueue(RabbitConfig.UPDATED_VERIFICATION_QUEUE, true);
    }

    private JsonNode receiveUpdated(long timeoutMs) throws Exception {
        Message m = rabbit.receive(RabbitConfig.UPDATED_VERIFICATION_QUEUE, timeoutMs);
        return m == null ? null : mapper.readTree(m.getBody());
    }

    @Test
    void oneOrderUpdatedMessageIsPublishedForASuccessfulEdit() throws Exception {
        long id = placeOrder(ACME);
        var res = asClient(HttpMethod.PUT, "/orders/" + id, orderOf(line("MON-27-4K", 4)), ACME);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);

        JsonNode msg = receiveUpdated(5_000);
        assertThat(msg).isNotNull();
        assertThat(msg.get("orderId").asLong()).isEqualTo(id);
        assertThat(msg.get("clientId").asLong()).isEqualTo(ACME);
        assertThat(msg.get("lines")).hasSize(1);
        assertThat(msg.get("lines").get(0).get("sku").asText()).isEqualTo("MON-27-4K");
        assertThat(msg.get("lines").get(0).get("quantity").asInt()).isEqualTo(4);
        assertThat(msg.get("grossCents").asLong()).isEqualTo(res.getBody().get("grossCents").asLong());
        assertThat(msg.get("discountBp").asInt()).isEqualTo(1000);
        assertThat(msg.get("netCents").asLong()).isEqualTo(res.getBody().get("netCents").asLong());
        assertThat(Instant.ofEpochMilli((long) (msg.get("timestamp").asDouble() * 1000)))
                .isEqualTo(Instant.parse(res.getBody().get("edits").get(0).get("editedAt").asText())
                        .truncatedTo(java.time.temporal.ChronoUnit.MILLIS));
        assertThat(receiveUpdated(500)).as("exactly one message").isNull();
    }

    @Test
    void nothingIsPublishedForRejectedEdits() throws Exception {
        long id = placeOrder(ACME);
        asClient(HttpMethod.PUT, "/orders/" + id, orderOf(line("NOPE", 1)), ACME);
        asClient(HttpMethod.PUT, "/orders/" + id, orderOf(line("MS-WL-PRO", 0)), ACME);
        asClient(HttpMethod.PUT, "/orders/" + id, orderOf(line("MS-WL-PRO", 1)), GLOBEX);
        advance(id);
        asClient(HttpMethod.PUT, "/orders/" + id, orderOf(line("MS-WL-PRO", 1)), ACME);
        assertThat(receiveUpdated(1_000)).isNull();
    }

    @Test
    void nothingIsPublishedWhenAnEditTransactionRollsBack() throws Exception {
        tx.executeWithoutResult(status -> {
            events.publishEvent(new com.computedeliver.orders.order.OrderUpdatedEvent(
                    1, ACME, List.of(), 0, 0, 0, Instant.now()));
            status.setRollbackOnly();
        });
        assertThat(receiveUpdated(1_000)).isNull();
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
