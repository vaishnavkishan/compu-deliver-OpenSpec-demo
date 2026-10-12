package com.computedeliver.orders.order;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "order_edits")
@Getter
@NoArgsConstructor
public class OrderEdit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Instant editedAt;

    OrderEdit(Instant editedAt) {
        this.editedAt = editedAt;
    }
}
