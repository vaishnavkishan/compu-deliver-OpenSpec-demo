package com.computedeliver.orders.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {

    private OrderDtos() {
    }

    /** Quantity is a BigDecimal so fractional input reaches validation and gets a per-line error. */
    public record LineRequest(String sku, BigDecimal quantity) {
    }

    public record PlaceOrderRequest(List<LineRequest> lines) {
    }

    public record LineView(String sku, String name, int quantity, long unitPriceCents, long subtotalCents) {
    }

    public record HistoryView(OrderStatus status, Instant changedAt) {
    }

    public record OrderView(
            long id,
            long clientId,
            String clientName,
            OrderStatus status,
            long grossCents,
            int discountBp,
            long netCents,
            int totalUnits,
            Instant createdAt,
            List<LineView> lines,
            List<HistoryView> history) {
    }
}
