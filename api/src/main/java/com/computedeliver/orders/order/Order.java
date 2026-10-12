package com.computedeliver.orders.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long clientId;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private long grossCents;
    private int discountBp;
    private long netCents;
    private Instant createdAt;
    @Version
    private long version;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id", nullable = false)
    private List<OrderLine> lines = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id", nullable = false)
    @OrderBy("changedAt ASC, id ASC")
    private List<StatusChange> history = new ArrayList<>();

    Order(long clientId, int discountBp, Instant now) {
        this.clientId = clientId;
        this.discountBp = discountBp;
        this.createdAt = now;
        this.status = OrderStatus.SUBMITTED;
        this.history.add(new StatusChange(OrderStatus.SUBMITTED, now));
    }

    /** Adds a line and recomputes the pricing snapshot from the lines added so far. */
    void addLine(String sku, int qty, long unitPriceCents) {
        OrderLine line = new OrderLine(sku, qty, unitPriceCents);
        lines.add(line);
        grossCents = Math.addExact(grossCents, line.subtotalCents());
        netCents = Pricing.netCents(grossCents, discountBp);
    }

    public int totalUnits() {
        return lines.stream().mapToInt(OrderLine::getQty).sum();
    }

    void advanceTo(OrderStatus target, Instant now) {
        if (!status.canAdvanceTo(target)) {
            throw new IllegalStateException("Cannot move order from " + status + " to " + target);
        }
        changeStatus(target, now);
    }

    void cancel(Instant now) {
        if (!status.isCancellable()) {
            throw new IllegalStateException("Cannot cancel an order that is " + status);
        }
        changeStatus(OrderStatus.CANCELLED, now);
    }

    private void changeStatus(OrderStatus target, Instant now) {
        status = target;
        history.add(new StatusChange(target, now));
    }
}
