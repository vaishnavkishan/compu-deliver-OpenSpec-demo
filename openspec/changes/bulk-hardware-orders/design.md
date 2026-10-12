# Design

## Context

Greenfield repo: no application code and no existing specs. See proposal.md for motivation. The catalog and contracts are a mocked external system, and there is no authentication, so the active role and client come from UI dropdowns. Warehouse and Invoicing are future services; this change only produces the `OrderIntaken` event they will consume.

## Goals / Non-Goals

**Goals:**
- Business rules (pricing, transitions, cancellation, client scoping) enforced on the server, not only hidden in the UI.
- A runnable stack (API, Postgres, RabbitMQ, portal) deployable locally on k3s / Rancher Desktop via Helm.
- A reliable `OrderIntaken` event that is never published for an order that was not committed.

**Non-Goals:**
- Real security. Role and client are self-declared, so this is not an access-control boundary.
- Pagination, a transactional outbox, or consumers of the event.
- Warehouse and Invoicing services and UIs.

## Decisions

**Stack:** Java 25, Spring Boot 3.5 (Web, Validation, Data JPA, Actuator, AMQP), Maven, Lombok, Flyway, springdoc-openapi. PostgreSQL 17 and RabbitMQ as backing services. React 19, TypeScript, Vite and Tailwind for the portal. JUnit 5, Mockito, AssertJ and Testcontainers (Postgres, RabbitMQ) for backend tests; Vitest and React Testing Library for the frontend. Docker multi-stage builds and a Helm chart for k3s / Rancher Desktop.

**Schema via Flyway.** All tables are created and seeded by versioned migrations, so a fresh database comes up identically everywhere. Hibernate DDL generation is off.

**Role passed as request headers.** The portal sends `X-Role` and, for Enterprise Client, `X-Client-Id` on every call. A Spring filter/interceptor validates them and exposes them to controllers. Every endpoint checks the role it allows and scopes queries by client id. `ENTERPRISE_CLIENT` and `WAREHOUSE_OPERATOR` are functional; other role values are rejected with 403. Alternative: enforce only in the UI, which would make the rules untestable and easy to bypass.

**Money as integer minor units.** Prices are stored in cents (`bigint`), and the discount percentage as integer basis points. Discount = round(gross * bp / 10000), and net = gross - discount. Rounding is half-up in integer or `BigDecimal` arithmetic, never floating point.

**Pricing snapshot on the order row.** `gross_cents`, `discount_bp`, `net_cents` are written at intake in the same transaction as the order lines. Line unit prices are also copied onto the lines, so later price changes do not alter history.

**Lifecycle as an ordered enum.** An ordered `OrderStatus` enum drives "advance one step". A transition is valid only if the target is the next ordinal, and `DELIVERED` and `CANCELLED` have no successor. Cancellation is a separate action, allowed only from `SUBMITTED` and `CONFIRMED`. Each transition writes a row to `order_status_history` with a timestamp in the same transaction.

**Optimistic locking.** `Order` carries a JPA `@Version` column. A concurrent cancel vs. advance on the same order results in one success and one `OptimisticLockingFailureException`, mapped to HTTP 409.

**Advance is a Warehouse Operator action.** A dedicated Warehouse service is out of scope, but `POST /orders/{id}/advance` lives in the Order API and is allowed only for `WAREHOUSE_OPERATOR`, so the demo can progress orders through the lifecycle from a minimal Operator view in the portal. A future Warehouse service can take over this responsibility.

**`OrderIntaken` event.** After an order is committed, an `OrderIntaken` JSON message (order id, client id, lines with SKU and quantity, gross, discount_bp, net, timestamp) is published to a durable topic exchange with routing key `order.intaken`. Publishing happens from a `@TransactionalEventListener(phase = AFTER_COMMIT)` so a rolled-back order never emits an event. Exchange, queue declarations and a bound queue for verification are declared by Spring AMQP config.

**Data model (sketch):**

```
 clients(id, name, discount_bp NULL)      products(sku, name, unit_price_cents)
 orders(id, client_id, status, gross_cents, discount_bp, net_cents, created_at, version)
 order_lines(id, order_id, sku, qty, unit_price_cents)
 order_status_history(id, order_id, status, changed_at)
```

A client with `discount_bp` NULL has no contract, and intake rejects it. The seed includes one such client so that path can be demonstrated.

**API shape (sketch):** `POST /orders`, `GET /orders` (client: own, operator: all), `GET /orders/{id}`, `POST /orders/{id}/cancel` (client), `POST /orders/{id}/advance` (operator), `GET /clients`, `GET /products`, plus Actuator health and springdoc OpenAPI docs. Responses use clear 4xx errors for rule violations (400 invalid, 403 wrong role or other client's order, 409 invalid transition or version conflict).

**Deployment.** The API and the portal each get a multi-stage Dockerfile. One Helm chart deploys the API, portal, PostgreSQL and RabbitMQ to k3s; the API uses Actuator liveness and readiness probes.

## UI/UX Reference

The visual reference is the static mockups in `figma-designs/` (`enterprise-user-*.html`, `create-order-page.html`, and `warehouse-ops-dashboard.html` for the queue table only). The implemented UI SHALL match their look and layout. Decisions on where the mockups and this plan differ:

- **Single page, same visuals.** The portal is one React app with a role-gated shell, reusing the mockups' layout and styling.
- **Five statuses.** The mockup stepper has 4 steps (Intake, Processing, Shipped, Delivered). The app shows 5 (Submitted, Confirmed, Processing, Shipped, Delivered), same styling with one extra circle.
- **Role names follow the specs** (Enterprise Client, Warehouse Operator, Invoice Department), not the mockup labels. Enterprise Client and Warehouse Operator are functional; Invoice Department is listed as unavailable.
- **Not implemented (out of scope):** the Invoice mockup entirely, the Warehouse mockup's fulfilment screens, bulk volume tier discount (only in `create-order-page.html`), editing line items after submit, asset serial tagging, carrier and tracking numbers, the ship gate, invoice generation, payment status, the invoice ledger, and the Facility dropdown. Do not build these.

**Visual tokens:** Tailwind (build-time via Vite), system sans-serif font, slate neutrals on a `#f8fafc` canvas, blue-600 (`#2563eb`, hover `#1d4ed8`) as the accent, emerald for success and discounts, red for danger. Page container is `max-w-7xl` centered. Cards are white, `rounded-xl`, 1px slate-200 border, light shadow, 24px padding. Tables use small uppercase slate-400 headers and divided rows with a hover tint.

**Header:** icon tile, title, portal pill, and subtitle on the left. On the right are a Role dropdown, the Company dropdown (Enterprise Client only), and a blue contract badge ("Contract # • Tier 1 (N% Discount)"). The icon tile is blue-600 for Enterprise Client and slate-900 for Warehouse Operator.

**Role-gated shell:** the Role dropdown lists Enterprise Client, Warehouse Operator and Invoice Department. Selecting Invoice Department shows a "coming soon" placeholder view with no actions.

**Warehouse Operator view (minimal):** the mockup's queue table of all orders (id, client, date, volume, net total, status badge) with a search box and count pill, and an "Advance" button on each non-terminal row showing the next status. No tagging, carrier, ship gate or fulfilment UI. Outcomes and errors (e.g. 409) appear as toasts.

**Status badge colors:** Submitted blue, Confirmed sky, Processing amber, Shipped indigo, Delivered emerald, Cancelled red.

**Stepper:** numbered circles on a thin connecting line. Completed steps are emerald, the current step is blue (pulsing), and future steps are slate-200. A cancelled order shows a red banner in place of the stepper.

**Enterprise Client dashboard:** action bar with a "+ Create New Order" button. Below it, an "Active Procurement Tracker" card with a count pill ("N Active") and one bordered card per active order (id, date, item count, net total, stepper, "View Order" link, and "Cancel" only when cancellable). Empty state is a dashed bordered message. Then an "Order History Log" table (Order ID, Submission Date, Volume, Net Total, Fulfillment Status, View Order) for delivered and cancelled orders.

**Order page (create and view):** back button, title with status badge, lifecycle tracker, then a two-column layout. Left (2/3): "Order Line Items" table with a searchable SKU/name dropdown per row (shows name, SKU chip and price, and "No matching products found"), a "Change" link once chosen, a quantity input (minimum 1), subtotal, and a remove icon, plus add-line buttons at top and bottom. Right (1/3), sticky: "Contract Net Pricing" showing total units, gross, contract discount with a percentage badge, "Logistics & Freight: Waived", and a large blue Final Net Total, then the submit button. A viewed order is read-only: inputs disabled and the submit button greyed out.

**Feedback:** cancel opens a centered confirmation modal (red icon, "Confirm Cancellation", "Keep Order" and "Yes, Cancel Request"). Outcomes and errors appear as a dark toast at bottom-right that fades after about 3 seconds.

## Risks / Trade-offs

- [Self-declared role means anyone can act as anyone] -> Accepted for a demo; stated as a non-goal and noted in the README.
- [Seed data standing in for a real external system may hide integration issues] -> Keep catalog and contract reads behind one small module, so a real client could replace it later.
- [Two actors racing on the same order, e.g. cancel vs. advance] -> `@Version` optimistic locking so only one wins and the other gets a 409.
- [Broker down after commit means the `OrderIntaken` event is lost] -> Accepted for now; a transactional outbox is the future fix. Publish failures are logged with the order id.
