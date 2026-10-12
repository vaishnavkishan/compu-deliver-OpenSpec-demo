package com.computedeliver.orders.order;

import java.time.Instant;
import java.util.List;

/** Application event raised inside the intake transaction; published to RabbitMQ only after commit. */
public record OrderIntakenEvent(
        long orderId,
        long clientId,
        List<Line> lines,
        long grossCents,
        int discountBp,
        long netCents,
        Instant timestamp) {

    public record Line(String sku, int quantity) {
    }
}
