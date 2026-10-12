package com.computedeliver.orders.reference;

import org.springframework.data.jpa.repository.JpaRepository;

interface ProductRepository extends JpaRepository<Product, String> {
}
