package com.computedeliver.orders.reference;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Single entry point for clients, contracts and the product catalog. These are seed data standing in
 * for an external system, so a real client can replace this class later.
 */
@Service
public class ReferenceData {

    private final ClientRepository clients;
    private final ProductRepository products;

    ReferenceData(ClientRepository clients, ProductRepository products) {
        this.clients = clients;
        this.products = products;
    }

    public List<Client> listClients() {
        return clients.findAll().stream().sorted((a, b) -> a.getId().compareTo(b.getId())).toList();
    }

    public List<Product> listProducts() {
        return products.findAll().stream().sorted((a, b) -> a.getSku().compareTo(b.getSku())).toList();
    }

    public Optional<Client> findClient(long id) {
        return clients.findById(id);
    }

    public Optional<Product> findProduct(String sku) {
        return products.findById(sku);
    }
}
