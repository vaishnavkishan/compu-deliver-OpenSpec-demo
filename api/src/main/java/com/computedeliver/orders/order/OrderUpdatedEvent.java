package com.computedeliver.orders.order;

import java.time.Instant;
import java.util.List;

/** Application event raised inside the edit transaction; published to RabbitMQ only after commit. */
public record OrderUpdatedEvent(
        long orderId,
        long clientId,
        List<OrderIntakenEvent.Line> lines,
        long grossCents,
        int discountBp,
        long netCents,
        Instant timestamp) {
}
