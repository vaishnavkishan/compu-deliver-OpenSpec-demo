# Tasks

## 1. Backend scaffold

- [x] 1.1 Scaffold a Maven Spring Boot 3.5 project on Java 25 (Web, Validation, Data JPA, Actuator, AMQP, Lombok, Flyway, springdoc-openapi, PostgreSQL driver) with JUnit 5, Mockito, AssertJ and Testcontainers; verify `mvn test` runs with an empty suite
- [x] 1.2 Add local Postgres 17 and RabbitMQ (docker compose) and app config; verify the app starts and Actuator `/actuator/health` is UP and springdoc docs load

## 2. Data layer and seed data

- [x] 2.1 Add Flyway migrations for clients, products, orders (with `version`), order_lines, order_status_history; verify migration applies on a fresh Testcontainers Postgres
- [x] 2.2 Seed clients (several discounts, including 0% and one with no contract) and products with unit prices via migration, read behind one small reference-data module; verify with a test covering the no-contract case
- [x] 2.3 Add `GET /clients` and `GET /products`; verify with API tests

## 3. Role handling

- [x] 3.1 Add a filter/interceptor reading `X-Role` and `X-Client-Id`, rejecting missing or invalid values, requiring a client id for Enterprise Client, allowing Enterprise Client and Warehouse Operator, and rejecting other roles with 403; verify with tests for each role and bad input
- [x] 3.2 Document the no-auth, self-declared role model in the README; verify the README states it is not a security boundary

## 4. Order intake and pricing

- [x] 4.1 Implement integer-cents pricing (gross, discount, net with half-up rounding); verify unit tests for the 10% example, 0% discount, and fractional-cent rounding
- [x] 4.2 Implement `POST /orders` with validation (non-empty lines, positive integer quantities, known SKUs), no-contract rejection, and a transactional snapshot of pricing and line prices; verify API tests for each scenario in the order-intake spec
- [x] 4.3 Verify a contract or price change after intake does not alter an existing order, with a test

## 5. Lifecycle and history

- [x] 5.1 Implement the ordered `OrderStatus` transition rule and `POST /orders/{id}/advance` for Warehouse Operator only; verify tests for valid, skip, backward, terminal, and non-operator-rejected cases, with a history row and timestamp per transition
- [x] 5.2 Record the initial `SUBMITTED` history entry at intake and return history with order detail; verify with tests
- [x] 5.3 Implement `GET /orders` (client: own only, Warehouse Operator: all) and `GET /orders/{id}` with client scoping; verify tests that client A cannot see client B's orders and that an empty history succeeds

## 6. Cancellation and concurrency

- [x] 6.1 Implement `POST /orders/{id}/cancel` for Enterprise Client only in `SUBMITTED` or `CONFIRMED`, with a history entry; verify tests for allowed states, blocked states, already cancelled, another client's order, and Warehouse Operator being rejected
- [x] 6.2 Map `OptimisticLockingFailureException` to 409; verify cancel and advance racing on one order yield one success and one 409 (Testcontainers)

## 7. OrderIntaken event

- [x] 7.1 Declare the durable topic exchange and a verification queue bound to `order.intaken` with Spring AMQP; verify the broker config in a Testcontainers RabbitMQ test
- [x] 7.2 Publish `OrderIntaken` from an `AFTER_COMMIT` transactional listener with the order id, client id, lines, gross, discount_bp, net and timestamp; verify tests for one message on success, none for rejected orders, and none on rollback

## 8. Portal UI

Reference: the Enterprise Client mockups in `figma-designs/` and the "UI/UX Reference" section of design.md. Single React app, same visuals; do not build mockup-only features, the Invoice view, or Warehouse fulfilment screens.

- [x] 8.1 Scaffold React 19 + TypeScript + Vite + Tailwind with Vitest and RTL, and an API client that sends `X-Role` and `X-Client-Id`; verify `npm test` runs and the dev server proxies to the API
- [x] 8.2 Build the role-gated shell: header with title, portal pill, Role dropdown, conditional Company dropdown, contract badge, and a "coming soon" placeholder for Invoice Department; verify with RTL tests and manually against `enterprise-user-dashboard.html`
- [x] 8.3 Build shared pieces: five-step stepper (pulsing current step, cancelled banner), status badges with the specified colors, toast, and confirmation modal; verify with component tests for each status
- [x] 8.4 Build the Enterprise Client dashboard: "+ Create New Order", Active Procurement Tracker with count pill and cards, Order History Log table, and cancel via modal; verify against `enterprise-user-dashboard.html`, including the empty state and that cancel appears only for `SUBMITTED` or `CONFIRMED`
- [x] 8.5 Build the order form and order detail view: product search picker, quantity, subtotal, remove, add line, sticky Contract Net Pricing panel (freight shown as Waived), read-only detail with transition timestamps; verify against `enterprise-user-detail-page.html` and that API errors (no contract, invalid line) show as toasts
- [x] 8.6 Build the minimal Warehouse Operator view (queue table of all orders with search and count pill, status badges, and an "Advance" button on non-terminal rows), styled like `warehouse-ops-dashboard.html` without tagging, shipping, or fulfilment features; verify with RTL tests and manually that advancing moves an order one step, no advance button shows on `DELIVERED` or `CANCELLED`, no cancel control appears, and a 409 shows as a toast

## 9. Packaging and deployment

- [x] 9.1 Add multi-stage Dockerfiles for the API and the portal; verify both images build and run
- [x] 9.2 Add a Helm chart deploying the API, portal, PostgreSQL and RabbitMQ with Actuator probes; verify `helm template` renders and the chart installs on k3s / Rancher Desktop

## 10. Integration check

- [x] 10.1 Run the full flow end to end on the deployed stack (place, see `OrderIntaken` on the queue, cancel, place, switch to Warehouse Operator and advance to DELIVERED, then view the history as the client) and verify behavior matches every scenario in the five specs
- [x] 10.2 Add run and deploy instructions and the seeded clients' discounts to the README; verify a fresh clone can follow them to a working app
