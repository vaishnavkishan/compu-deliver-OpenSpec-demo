package com.computedeliver.orders.events;

import com.computedeliver.orders.order.OrderUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Publishes {@code OrderUpdated} only after the edit transaction commits; failures are logged with the id. */
@Component
class OrderUpdatedPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderUpdatedPublisher.class);

    private final RabbitTemplate rabbit;

    OrderUpdatedPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onOrderUpdated(OrderUpdatedEvent event) {
        try {
            rabbit.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ORDER_UPDATED_KEY, event);
        } catch (RuntimeException e) {
            log.error("Failed to publish OrderUpdated for order {}", event.orderId(), e);
        }
    }
}
