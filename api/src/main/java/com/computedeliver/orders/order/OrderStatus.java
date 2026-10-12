package com.computedeliver.orders.order;

import java.util.Optional;

/** Declared in lifecycle order; {@code CANCELLED} sits outside the forward path. */
public enum OrderStatus {
    SUBMITTED,
    CONFIRMED,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public boolean isTerminal() {
        return this == DELIVERED || this == CANCELLED;
    }

    /** The only status an order in this status may be advanced to, if any. */
    public Optional<OrderStatus> next() {
        return isTerminal() ? Optional.empty() : Optional.of(values()[ordinal() + 1]);
    }

    public boolean canAdvanceTo(OrderStatus target) {
        return next().filter(n -> n == target).isPresent();
    }

    public boolean isCancellable() {
        return this == SUBMITTED || this == CONFIRMED;
    }
}
