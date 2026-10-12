# Spec Delta

## Purpose

Lets enterprise clients review their own past and active orders and cancel orders that have not yet entered processing, while giving Invoice Department a read-only view of all orders.

## ADDED Requirements

### Requirement: Client order history
The system SHALL show an Enterprise Client the list of their own orders, including status, gross, discount percentage, net, and lines. The system SHALL NOT show orders belonging to other clients.

#### Scenario: Own orders only
- **WHEN** client A is the acting client and client B has placed orders
- **THEN** only client A's orders are listed

#### Scenario: Empty history
- **WHEN** the acting client has placed no orders
- **THEN** an empty history is shown without error

#### Scenario: Order detail
- **WHEN** a client opens one of their orders
- **THEN** its lines, pricing snapshot, current status, and transition timestamps are shown

### Requirement: Client cancellation
The system SHALL allow an Enterprise Client to cancel their own order only while its status is `SUBMITTED` or `CONFIRMED`. Cancellation SHALL take effect immediately, incur no fee, and set the status to `CANCELLED`.

#### Scenario: Cancel submitted order
- **WHEN** a client cancels their `SUBMITTED` order
- **THEN** the status becomes `CANCELLED` immediately

#### Scenario: Cancel confirmed order
- **WHEN** a client cancels their `CONFIRMED` order
- **THEN** the status becomes `CANCELLED` immediately

#### Scenario: Cancel after processing begins
- **WHEN** a client tries to cancel an order that is `PROCESSING`, `SHIPPED`, or `DELIVERED`
- **THEN** the request is rejected with an error and the status is unchanged

#### Scenario: Cancel already cancelled
- **WHEN** a client tries to cancel an order that is already `CANCELLED`
- **THEN** the request is rejected and the status is unchanged

#### Scenario: Cancel another client's order
- **WHEN** a client tries to cancel an order belonging to a different client
- **THEN** the request is rejected and the order is unchanged

### Requirement: Invoice Department read-only view
The system SHALL show the Invoice Department role a read-only list of all orders across clients, including status and pricing snapshot, with no actions available.

#### Scenario: All orders visible
- **WHEN** the Invoice Department role is active
- **THEN** orders from every client are listed with status and pricing
