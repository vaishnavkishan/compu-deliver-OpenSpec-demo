package com.computedeliver.orders.events;

import com.computedeliver.orders.order.OrderIntakenEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Publishes {@code OrderIntaken} only after the order transaction commits, so a rolled-back order never
 * emits an event. A broker failure after commit loses the event (no outbox yet); it is logged with the id.
 */
@Component
class OrderIntakenPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderIntakenPublisher.class);

    private final RabbitTemplate rabbit;

    OrderIntakenPublisher(RabbitTemplate rabbit) {
        this.rabbit = rabbit;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onOrderIntaken(OrderIntakenEvent event) {
        try {
            rabbit.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ORDER_INTAKEN_KEY, event);
        } catch (RuntimeException e) {
            log.error("Failed to publish OrderIntaken for order {}", event.orderId(), e);
        }
    }
}
