# Proposal

## Why

Enterprise clients buy hardware in bulk under pre-negotiated contract discounts, and there is no system to accept those orders, compute the correct net total, or show where an order stands. This change establishes the client-facing order flow: intake with contract pricing, a status lifecycle, client self-service history and cancellation, and an `OrderIntaken` event so downstream systems (warehouse, invoicing) can be built later.

## What Changes

- Add order intake: an Enterprise Client submits an order of SKU + quantity lines; the system computes gross, applies the client's flat contract discount percentage, and stores `gross`, `discount_pct`, and `net` as a snapshot on the order.
- Reject orders from clients that have no contract.
- Publish an `OrderIntaken` event to RabbitMQ after an order is successfully committed.
- Add an order status lifecycle: `SUBMITTED > CONFIRMED > PROCESSING > SHIPPED > DELIVERED`, advanced one step at a time by a Warehouse Operator through a minimal demo view, with a timestamp recorded per transition.
- Allow an Enterprise Client to cancel an order only while it is `SUBMITTED` or `CONFIRMED` (instant, no fee). `CANCELLED` and `DELIVERED` are terminal.
- Allow an Enterprise Client to view their own order history only.
- Allow an Enterprise Client to edit the lines of their own order only while it is `SUBMITTED` (the Intake step). Pricing is recomputed from the current contract and prices, replacing the snapshot, and an `OrderUpdated` event is published after commit. Once the order leaves `SUBMITTED` it can no longer be edited.
- Allow a Warehouse Operator to open a read-only detail view of any order (client, lines, pricing snapshot, status, transition timestamps).
- Add a role-gated portal shell (no authentication): Enterprise Client (with a second selector for the seeded client) and a minimal Warehouse Operator view (all orders, advance status only) are functional. Invoice Department appears as an unavailable future role.
- Seed mock data standing in for an external system: clients with discount percentages, and products with unit prices.

**In scope:** Order API and database (intake, pricing, editing while `SUBMITTED`, status lifecycle, cancellation, contract terms, history), the RabbitMQ broker, and the Portal UI (Enterprise Client dashboard, order create/detail views, role-gated shell, and a minimal Warehouse Operator queue with an advance button so the demo can progress orders).

**Out of scope (future):** a dedicated Warehouse API/DB and the full Operator UI (tagging, shipping, fulfilment); Invoicing API/DB and Staff UI; authentication; asset tagging and shipping logic; a real product catalog or contract system.

## Capabilities

### New Capabilities
- `role-selection`: Role-gated portal shell: choosing the active role (and client, for Enterprise Client) without authentication.
- `order-intake`: Submitting bulk orders and computing net totals from the client's flat contract discount, snapshotted on the order.
- `order-lifecycle`: Order statuses, valid forward transitions, terminal states, and per-transition timestamps.
- `order-history-cancel`: Client-scoped order history and client cancellation rules.
- `order-events`: Publishing the `OrderIntaken` and `OrderUpdated` events to RabbitMQ.
- `order-edit`: Client editing of an order's lines while it is `SUBMITTED`.

### Modified Capabilities

None. The project has no existing specs.

## Impact

- Greenfield: no existing application code. This introduces the Order API (Java 25, Spring Boot 3.5, Maven, Flyway, springdoc-openapi), PostgreSQL 17 persistence with `@Version` optimistic locking, a RabbitMQ broker, and a React 19 + TypeScript + Vite + Tailwind portal.
- Delivery: multi-stage Docker images and a Helm chart for k3s / Rancher Desktop.
- Testing: JUnit 5, Mockito, AssertJ and Testcontainers on the backend; Vitest and React Testing Library on the frontend.
- The catalog and contracts are mocked locally as seed data.
- The UI must match the Enterprise Client mockups in `figma-designs/` (layout, styling, status colors, flows). Mockup-only features such as asset tagging and invoicing remain out of scope, as does the Invoice mockup. The Warehouse mockup is followed only for the queue table, search and badges.
