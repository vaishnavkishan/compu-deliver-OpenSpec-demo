# Design

## Context

Greenfield repo: no application code, no existing specs, and no stack constraints in `openspec/config.yaml`. See proposal.md for motivation. The catalog and contracts are a mocked external system, and there is no authentication, so the active role and client come from UI dropdowns.

## Goals / Non-Goals

**Goals:**
- A small, runnable demo that is easy to read and show off the full order flow.
- Business rules (pricing, transitions, cancellation, client scoping) enforced on the server, not only hidden in the UI.

**Non-Goals:**
- Real security. Role and client are self-declared, so this is not an access-control boundary.
- Concurrency hardening, pagination, or deployment setup beyond local run.

## Decisions

**Stack (assumption, easy to change before apply): TypeScript, Node, Express, SQLite (`better-sqlite3`), and a single-page UI served statically by the same server.** One process, no external services, and a file database that seeds itself. Alternatives: a full-stack framework such as Next.js (heavier for a demo), or in-memory state (loses data on restart, and makes the history demo weaker).

**Role passed as request headers.** The UI sends `X-Role` and, for Enterprise Client, `X-Client-Id` on every call. A small middleware validates them and attaches them to the request. Every route checks the role it allows and scopes queries by client id. This keeps the no-auth model honest and keeps rules testable at the API level. Alternative: enforce only in the UI, which would make the rules untestable and easy to bypass.

**Money as integer minor units.** Prices are stored in cents, and the discount percentage is stored as an integer in basis points or a fixed-decimal value. Discount = round(gross * pct), and net = gross - discount. This avoids floating-point drift, and the spec requires it.

**Pricing snapshot on the order row.** `gross_cents`, `discount_bp`, `net_cents` are written at intake in the same transaction as the order lines. Line unit prices are also copied onto the lines, so later price changes do not alter history.

**Lifecycle as an ordered list.** A single ordered status array drives "advance one step". A transition is valid only if the target is the next index, and `DELIVERED` and `CANCELLED` have no successor. Cancellation is a separate action, allowed only from `SUBMITTED` and `CONFIRMED`. The transition check and update run in one transaction and write a row to an `order_status_history` table with a timestamp.

**Data model (sketch):**

```
 clients(id, name, discount_bp NULL)      products(sku, name, unit_price_cents)
 orders(id, client_id, status, gross_cents, discount_bp, net_cents, created_at)
 order_lines(id, order_id, sku, qty, unit_price_cents)
 order_status_history(id, order_id, status, changed_at)
```

A client with `discount_bp` NULL has no contract, and intake rejects it. The seed includes one such client so that path can be demonstrated.

**API shape (sketch):** `POST /orders`, `GET /orders` (client: own, invoice: all), `GET /orders/:id`, `POST /orders/:id/advance` (warehouse), `POST /orders/:id/cancel` (client), `GET /clients`, `GET /products`. Responses use clear 4xx errors for rule violations (400 invalid, 403 wrong role or other client's order, 409 invalid transition).

## UI/UX Reference

The visual reference is the static mockups in `figma-designs/` (`enterprise-user-*.html`, `create-order-page.html`, `warehouse-ops-*.html`, `invoice-staff-*.html`). The implemented UI SHALL match their look and layout. Decisions on where the mockups and this plan differ:

- **Single page, same visuals.** The mockups are separate pages per role; the app stays one page and swaps views on role change, reusing the mockups' layout and styling.
- **Five statuses.** The mockup stepper has 4 steps (Intake, Processing, Shipped, Delivered). The app shows 5 (Submitted, Confirmed, Processing, Shipped, Delivered), same styling with one extra circle.
- **Role names follow the specs** (Enterprise Client, Warehouse Operator, Invoice Department), not the mockup labels "Enterprise User" and "Invoice Employee".
- **Not implemented (out of scope, visual shell only where noted):** bulk volume tier discount (only in `create-order-page.html`), editing line items after submit, asset serial tagging, carrier and tracking numbers, the ship gate, invoice generation, payment status, the invoice ledger, and the Facility dropdown. Do not build these. The Warehouse view keeps the mockup's queue table, search box and badges, with an advance button in place of "Fulfill".

**Visual tokens:** Tailwind (CDN is fine for the demo), system sans-serif font, slate neutrals on a `#f8fafc` canvas, blue-600 (`#2563eb`, hover `#1d4ed8`) as the accent, emerald for success and discounts, red for danger. Page container is `max-w-7xl` centered. Cards are white, `rounded-xl`, 1px slate-200 border, light shadow, 24px padding. Tables use small uppercase slate-400 headers and divided rows with a hover tint.

**Header (all roles):** icon tile, title, portal pill, and subtitle on the left. On the right are a Role dropdown, the Company dropdown (Enterprise Client only), and a blue contract badge ("Contract # • Tier 1 (N% Discount)"). The icon tile color follows the role: blue-600 Enterprise Client, slate-900 Warehouse Operator, purple-700 Invoice Department.

**Status badge colors:** Submitted blue, Confirmed sky, Processing amber, Shipped indigo, Delivered emerald, Cancelled red.

**Stepper:** numbered circles on a thin connecting line. Completed steps are emerald, the current step is blue (pulsing), and future steps are slate-200. A cancelled order shows a red banner in place of the stepper.

**Enterprise Client dashboard:** action bar with a "+ Create New Order" button. Below it, an "Active Procurement Tracker" card with a count pill ("N Active") and one bordered card per active order (id, date, item count, net total, stepper, "View Order" link, and "Cancel" only when cancellable). Empty state is a dashed bordered message. Then an "Order History Log" table (Order ID, Submission Date, Volume, Net Total, Fulfillment Status, View Order) for delivered and cancelled orders.

**Order page (create and view):** back button, title with status badge, lifecycle tracker, then a two-column layout. Left (2/3): "Order Line Items" table with a searchable SKU/name dropdown per row (shows name, SKU chip and price, and "No matching products found"), a "Change" link once chosen, a quantity input (minimum 1), subtotal, and a remove icon, plus add-line buttons at top and bottom. Right (1/3), sticky: "Contract Net Pricing" showing total units, gross, contract discount with a percentage badge, "Logistics & Freight: Waived", and a large blue Final Net Total, then the submit button. A viewed order is read-only: inputs disabled and the submit button greyed out.

**Feedback:** cancel opens a centered confirmation modal (red icon, "Confirm Cancellation", "Keep Order" and "Yes, Cancel Request"). Outcomes and errors appear as a dark toast at bottom-right that fades after about 3 seconds.

## Risks / Trade-offs

- [Self-declared role means anyone can act as anyone] -> Accepted for a demo; stated as a non-goal and noted in the README.
- [Stack assumption may not match the user's preference] -> Called out here so it can be changed before `/opsx:apply`.
- [Seed data standing in for a real external system may hide integration issues] -> Keep catalog and contract reads behind one small module, so a real client could replace it later.
- [Two actors racing on the same order, e.g. cancel vs. advance] -> Do the status check and write in a single transaction so only one wins and the other gets a 409.
