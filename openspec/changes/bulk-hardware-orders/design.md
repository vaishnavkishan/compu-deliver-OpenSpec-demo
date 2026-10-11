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

## Risks / Trade-offs

- [Self-declared role means anyone can act as anyone] -> Accepted for a demo; stated as a non-goal and noted in the README.
- [Stack assumption may not match the user's preference] -> Called out here so it can be changed before `/opsx:apply`.
- [Seed data standing in for a real external system may hide integration issues] -> Keep catalog and contract reads behind one small module, so a real client could replace it later.
- [Two actors racing on the same order, e.g. cancel vs. advance] -> Do the status check and write in a single transaction so only one wins and the other gets a 409.
