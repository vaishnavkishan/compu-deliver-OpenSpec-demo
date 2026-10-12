# Proposal

## Why

Enterprise clients buy hardware in bulk under pre-negotiated contract discounts, and there is no system to accept those orders, compute the correct net total, or show where an order stands. This demo app establishes the core order flow: intake with contract pricing, warehouse-driven lifecycle tracking, and client self-service history and cancellation.

## What Changes

- Add order intake: an Enterprise Client submits an order of SKU + quantity lines; the system computes gross, applies the client's flat contract discount percentage, and stores `gross`, `discount_pct`, and `net` as a snapshot on the order.
- Reject orders from clients that have no contract.
- Add an order lifecycle: `SUBMITTED > CONFIRMED > PROCESSING > SHIPPED > DELIVERED`, advanced forward one step at a time by a Warehouse Operator, with a timestamp recorded per transition.
- Allow an Enterprise Client to cancel an order only while it is `SUBMITTED` or `CONFIRMED` (instant, no fee). `CANCELLED` and `DELIVERED` are terminal.
- Allow an Enterprise Client to view their own order history only.
- Add a role selector (no authentication): Enterprise Client (with a second selector for the seeded client), Warehouse Operator, and Invoice Department. Invoice Department gets a read-only view of all orders with no actions.
- Seed mock data standing in for an external system: clients with discount percentages, and products with unit prices.

Out of scope: authentication, asset tagging and shipping UI/logic, invoicing, and a real product catalog or contract system.

## Capabilities

### New Capabilities
- `role-selection`: Choosing the active role (and client, for Enterprise Client) without authentication, and the views and actions each role has.
- `order-intake`: Submitting bulk orders and computing net totals from the client's flat contract discount, snapshotted on the order.
- `order-lifecycle`: Order statuses, valid forward transitions by the Warehouse Operator, terminal states, and per-transition timestamps.
- `order-history-cancel`: Client-scoped order history and client cancellation rules.

### Modified Capabilities

None. The project has no existing specs.

## Impact

- Greenfield: no existing application code. This introduces the whole demo app (UI, order logic, persistence) plus seed data for clients, discounts, and products.
- Tech stack is not yet decided; it will be chosen in design.
- No external dependencies or integrations; the catalog and contracts are mocked locally.
- The UI must match the mockups in `figma-designs/` (layout, styling, status colors, flows). Features that appear only in the mockups, such as asset tagging, invoicing and order editing, remain out of scope.
