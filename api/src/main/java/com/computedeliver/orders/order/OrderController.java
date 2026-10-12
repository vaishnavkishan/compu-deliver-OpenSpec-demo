package com.computedeliver.orders.order;

import com.computedeliver.orders.order.OrderDtos.OrderView;
import com.computedeliver.orders.order.OrderDtos.PlaceOrderRequest;
import com.computedeliver.orders.security.Caller;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderController {

    private final OrderService service;

    OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView place(@RequestAttribute(Caller.ATTRIBUTE) Caller caller,
                           @RequestBody(required = false) PlaceOrderRequest request) {
        return service.place(caller, request);
    }

    @GetMapping("/orders")
    public List<OrderView> list(@RequestAttribute(Caller.ATTRIBUTE) Caller caller) {
        return service.list(caller);
    }

    @GetMapping("/orders/{id}")
    public OrderView get(@RequestAttribute(Caller.ATTRIBUTE) Caller caller, @PathVariable long id) {
        return service.get(caller, id);
    }

    @PostMapping("/orders/{id}/advance")
    public OrderView advance(@RequestAttribute(Caller.ATTRIBUTE) Caller caller, @PathVariable long id) {
        return service.advance(caller, id);
    }

    @PostMapping("/orders/{id}/cancel")
    public OrderView cancel(@RequestAttribute(Caller.ATTRIBUTE) Caller caller, @PathVariable long id) {
        return service.cancel(caller, id);
    }
}
