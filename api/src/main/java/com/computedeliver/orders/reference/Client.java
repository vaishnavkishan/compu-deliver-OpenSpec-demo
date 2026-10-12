package com.computedeliver.orders.reference;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "clients")
@Getter
@NoArgsConstructor
public class Client {

    @Id
    private Long id;
    private String name;
    /** Flat contract discount in basis points; null means the client has no contract. */
    private Integer discountBp;

    public boolean hasContract() {
        return discountBp != null;
    }
}
