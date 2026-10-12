package com.computedeliver.orders.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.computedeliver.orders.PostgresTestBase;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.beans.factory.annotation.Autowired;

class RabbitConfigTest extends PostgresTestBase {

    @Autowired
    AmqpAdmin admin;

    @Test
    void verificationQueueIsDeclaredOnTheBroker() {
        Properties props = admin.getQueueProperties(RabbitConfig.VERIFICATION_QUEUE);
        assertThat(props).isNotNull();
    }

    @Test
    void exchangeIsDurableTopicAndQueueIsBound() throws Exception {
        var res = RABBIT.execInContainer("rabbitmqctl", "list_exchanges", "name", "type", "durable", "--formatter", "json");
        assertThat(res.getStdout()).contains("\"name\":\"orders.events\"").contains("\"type\":\"topic\"")
                .containsPattern("orders\\.events\"[^}]*\"durable\":true|\"durable\":true[^}]*orders\\.events");
        var bindings = RABBIT.execInContainer("rabbitmqctl", "list_bindings", "--formatter", "json");
        assertThat(bindings.getStdout()).contains("orders.order-intaken.verification").contains("order.intaken");
    }
}
