package com.computedeliver.orders.order;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_lines")
@Getter
@NoArgsConstructor
public class OrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String sku;
    private int qty;
    private long unitPriceCents;

    OrderLine(String sku, int qty, long unitPriceCents) {
        this.sku = sku;
        this.qty = qty;
        this.unitPriceCents = unitPriceCents;
    }

    public long subtotalCents() {
        return Math.multiplyExact(unitPriceCents, (long) qty);
    }
}
