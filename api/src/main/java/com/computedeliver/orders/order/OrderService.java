package com.computedeliver.orders.order;

import com.computedeliver.orders.order.OrderDtos.EditView;
import com.computedeliver.orders.order.OrderDtos.HistoryView;
import com.computedeliver.orders.order.OrderDtos.LineRequest;
import com.computedeliver.orders.order.OrderDtos.LineView;
import com.computedeliver.orders.order.OrderDtos.OrderView;
import com.computedeliver.orders.order.OrderDtos.PlaceOrderRequest;
import com.computedeliver.orders.reference.Client;
import com.computedeliver.orders.reference.Product;
import com.computedeliver.orders.reference.ReferenceData;
import com.computedeliver.orders.security.Caller;
import com.computedeliver.orders.security.Role;
import com.computedeliver.orders.web.ApiException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final ReferenceData referenceData;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    OrderService(OrderRepository orders, ReferenceData referenceData, ApplicationEventPublisher events, Clock clock) {
        this.orders = orders;
        this.referenceData = referenceData;
        this.events = events;
        this.clock = clock;
    }

    @Transactional
    public OrderView place(Caller caller, PlaceOrderRequest request) {
        caller.requireRole(Role.ENTERPRISE_CLIENT);
        Client client = requireContractClient(caller);
        requireLines(request);

        Order order = new Order(client.getId(), client.getDiscountBp(), clock.instant());
        List<OrderIntakenEvent.Line> eventLines = addLines(order, request);

        Order saved = orders.save(order);
        events.publishEvent(new OrderIntakenEvent(saved.getId(), saved.getClientId(), eventLines,
                saved.getGrossCents(), saved.getDiscountBp(), saved.getNetCents(), saved.getCreatedAt()));
        return toView(saved);
    }

    @Transactional
    public OrderView edit(Caller caller, long id, PlaceOrderRequest request) {
        caller.requireRole(Role.ENTERPRISE_CLIENT);
        Order order = orders.findById(id).orElseThrow(() -> notFound(id));
        if (order.getClientId() != caller.requireClientId()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Order " + id + " belongs to a different client");
        }
        if (order.getStatus() != OrderStatus.SUBMITTED) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Order " + id + " is " + order.getStatus() + " and can no longer be edited");
        }
        Client client = requireContractClient(caller);
        requireLines(request);

        order.startEdit(client.getDiscountBp(), clock.instant());
        List<OrderIntakenEvent.Line> eventLines = addLines(order, request);

        Order saved = orders.saveAndFlush(order);
        Instant editedAt = saved.getEdits().get(saved.getEdits().size() - 1).getEditedAt();
        events.publishEvent(new OrderUpdatedEvent(saved.getId(), saved.getClientId(), eventLines,
                saved.getGrossCents(), saved.getDiscountBp(), saved.getNetCents(), editedAt));
        return toView(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderView> list(Caller caller) {
        List<Order> found = caller.isOperator()
                ? orders.findAllByOrderByIdDesc()
                : orders.findByClientIdOrderByIdDesc(caller.requireClientId());
        return found.stream().map(this::toView).toList();
    }

    @Transactional(readOnly = true)
    public OrderView get(Caller caller, long id) {
        return toView(loadVisible(caller, id));
    }

    @Transactional
    public OrderView advance(Caller caller, long id) {
        caller.requireRole(Role.WAREHOUSE_OPERATOR);
        Order order = orders.findById(id).orElseThrow(() -> notFound(id));
        OrderStatus target = order.getStatus().next().orElseThrow(() -> new ApiException(HttpStatus.CONFLICT,
                "Order " + id + " is " + order.getStatus() + " and cannot be advanced"));
        order.advanceTo(target, clock.instant());
        return toView(orders.saveAndFlush(order));
    }

    @Transactional
    public OrderView cancel(Caller caller, long id) {
        caller.requireRole(Role.ENTERPRISE_CLIENT);
        Order order = orders.findById(id).orElseThrow(() -> notFound(id));
        if (order.getClientId() != caller.requireClientId()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Order " + id + " belongs to a different client");
        }
        if (!order.getStatus().isCancellable()) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Order " + id + " is " + order.getStatus() + " and can no longer be cancelled");
        }
        order.cancel(clock.instant());
        return toView(orders.saveAndFlush(order));
    }

    private Client requireContractClient(Caller caller) {
        Client client = referenceData.findClient(caller.requireClientId())
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Unknown client"));
        if (!client.hasContract()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "No contract exists for " + client.getName() + "; orders cannot be placed");
        }
        return client;
    }

    private static void requireLines(PlaceOrderRequest request) {
        if (request == null || request.lines() == null || request.lines().isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "An order needs at least one line");
        }
    }

    private List<OrderIntakenEvent.Line> addLines(Order order, PlaceOrderRequest request) {
        List<OrderIntakenEvent.Line> eventLines = new ArrayList<>();
        int lineNo = 0;
        for (LineRequest line : request.lines()) {
            lineNo++;
            int qty = parseQuantity(line, lineNo);
            Product product = line.sku() == null ? null : referenceData.findProduct(line.sku()).orElse(null);
            if (product == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Line " + lineNo + ": unknown SKU " + line.sku());
            }
            order.addLine(product.getSku(), qty, product.getUnitPriceCents());
            eventLines.add(new OrderIntakenEvent.Line(product.getSku(), qty));
        }
        return eventLines;
    }

    private Order loadVisible(Caller caller, long id) {
        Order order = orders.findById(id).orElseThrow(() -> notFound(id));
        if (caller.isClient() && order.getClientId() != caller.requireClientId()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Order " + id + " belongs to a different client");
        }
        return order;
    }

    private static int parseQuantity(LineRequest line, int lineNo) {
        BigDecimal q = line.quantity();
        if (q == null || q.signum() <= 0 || q.stripTrailingZeros().scale() > 0 || q.compareTo(BigDecimal.valueOf(1_000_000)) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Line " + lineNo + ": quantity must be a positive whole number");
        }
        return q.intValueExact();
    }

    private static ApiException notFound(long id) {
        return new ApiException(HttpStatus.NOT_FOUND, "Order " + id + " not found");
    }

    private OrderView toView(Order o) {
        String clientName = referenceData.findClient(o.getClientId()).map(Client::getName).orElse("Unknown");
        List<LineView> lines = o.getLines().stream().map(l -> new LineView(l.getSku(),
                referenceData.findProduct(l.getSku()).map(Product::getName).orElse(l.getSku()),
                l.getQty(), l.getUnitPriceCents(), l.subtotalCents())).toList();
        List<HistoryView> history = o.getHistory().stream()
                .map(h -> new HistoryView(h.getStatus(), h.getChangedAt())).toList();
        return new OrderView(o.getId(), o.getClientId(), clientName, o.getStatus(), o.getGrossCents(),
                o.getDiscountBp(), o.getNetCents(), o.totalUnits(), o.getCreatedAt(), lines, history,
                o.getEdits().stream().map(e -> new EditView(e.getEditedAt())).toList());
    }
}
