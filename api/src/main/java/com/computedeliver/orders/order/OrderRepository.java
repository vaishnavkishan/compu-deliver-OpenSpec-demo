package com.computedeliver.orders.order;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByClientIdOrderByIdDesc(long clientId);

    List<Order> findAllByOrderByIdDesc();
}
