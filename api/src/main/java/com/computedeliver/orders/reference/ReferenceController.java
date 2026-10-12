package com.computedeliver.orders.reference;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReferenceController {

    public record ClientView(long id, String name, Integer discountBp, boolean hasContract) {
    }

    public record ProductView(String sku, String name, long unitPriceCents) {
    }

    private final ReferenceData referenceData;

    ReferenceController(ReferenceData referenceData) {
        this.referenceData = referenceData;
    }

    @GetMapping("/clients")
    public List<ClientView> clients() {
        return referenceData.listClients().stream()
                .map(c -> new ClientView(c.getId(), c.getName(), c.getDiscountBp(), c.hasContract()))
                .toList();
    }

    @GetMapping("/products")
    public List<ProductView> products() {
        return referenceData.listProducts().stream()
                .map(p -> new ProductView(p.getSku(), p.getName(), p.getUnitPriceCents()))
                .toList();
    }
}
