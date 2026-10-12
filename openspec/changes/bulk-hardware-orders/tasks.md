# Tasks

## 1. Project setup

- [ ] 1.1 Scaffold a TypeScript Node project with Express, better-sqlite3, and a test runner (Vitest); verify `npm install` and `npm test` run with an empty suite
- [ ] 1.2 Add `npm start` serving the API and static UI; verify the server starts and responds on a health route

## 2. Data layer and seed data

- [ ] 2.1 Create schema for clients, products, orders, order_lines, order_status_history; verify the schema creates on a fresh database in a test
- [ ] 2.2 Seed clients (several discounts, including 0% and one with no contract) and products with unit prices behind one small reference-data module; verify with a test that seeding is idempotent and covers the no-contract case
- [ ] 2.3 Add `GET /clients` and `GET /products`; verify with API tests

## 3. Role handling

- [ ] 3.1 Add middleware reading `X-Role` and `X-Client-Id`, rejecting missing or invalid values and requiring a client id for Enterprise Client; verify with tests for each role and bad input
- [ ] 3.2 Document the no-auth, self-declared role model in the README; verify the README states it is not a security boundary

## 4. Order intake and pricing

- [ ] 4.1 Implement integer-cents pricing (gross, discount, net with rounding); verify unit tests for the 10% example, 0% discount, and fractional-cent rounding
- [ ] 4.2 Implement `POST /orders` with validation (non-empty lines, positive integer quantities, known SKUs), no-contract rejection, and a transactional snapshot of pricing and line prices; verify API tests for each scenario in the order-intake spec
- [ ] 4.3 Verify a contract or price change after intake does not alter an existing order, with a test

## 5. Lifecycle and history

- [ ] 5.1 Implement `POST /orders/:id/advance` for Warehouse Operator (one step forward only, terminal states rejected, history row with timestamp); verify tests for valid, skip, backward, and terminal cases, and for non-operator roles being rejected
- [ ] 5.2 Record the initial `SUBMITTED` history entry at intake and return history with order detail; verify with tests
- [ ] 5.3 Implement `GET /orders` (client: own only, Invoice Department: all, Warehouse Operator: all) and `GET /orders/:id` with client scoping; verify tests that client A cannot see client B's orders and that an empty history succeeds

## 6. Cancellation

- [ ] 6.1 Implement `POST /orders/:id/cancel` for Enterprise Client only in `SUBMITTED` or `CONFIRMED`, with a history entry; verify tests for allowed states, blocked states, already cancelled, another client's order, and Warehouse Operator being rejected
- [ ] 6.2 Verify cancel and advance racing on one order result in one success and one 409, using a transactional test

## 7. UI

Reference: the mockups in `figma-designs/` and the "UI/UX Reference" section of design.md. Single page, same visuals; do not build mockup-only features.

- [ ] 7.1 Set up Tailwind and the page shell: header with title, portal pill, Role dropdown, conditional Company dropdown, contract badge, and per-role icon color; verify manually that the client dropdown shows only for Enterprise Client, switching it changes the scoped data, and the header matches `enterprise-user-dashboard.html`
- [ ] 7.2 Build shared UI pieces: five-step stepper (pulsing current step, cancelled banner), status badges with the specified colors, toast, and confirmation modal; verify manually that each status renders the right colors
- [ ] 7.3 Build the Enterprise Client dashboard: "+ Create New Order", Active Procurement Tracker with count pill and cards, Order History Log table, and cancel via modal; verify manually against `enterprise-user-dashboard.html`, including the empty state and that cancel appears only for `SUBMITTED` or `CONFIRMED`
- [ ] 7.4 Build the order form and order detail view: product search picker, quantity, subtotal, remove, add line, sticky Contract Net Pricing panel (freight shown as Waived), read-only detail with transition timestamps; verify manually against `enterprise-user-detail-page.html` and that API errors (no contract, invalid line) show as toasts
- [ ] 7.5 Build the Warehouse Operator view (queue table with search and count pill, advance button, badges) and the Invoice Department read-only view (all orders, no actions), styled like `warehouse-ops-dashboard.html` and `invoice-staff-dashboard.html` without tagging, shipping, or invoicing features; verify manually that no cancel or advance controls appear for Invoice Department

## 8. Integration check

- [ ] 8.1 Run the full flow end to end in the running app (place, cancel, place, advance to DELIVERED, view as Invoice Department) and verify behavior matches every scenario in the four specs
- [ ] 8.2 Add run instructions and the seeded clients' discounts to the README; verify a fresh clone can follow them to a working app
