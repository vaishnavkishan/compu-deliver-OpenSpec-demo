# Spec Delta

## Purpose

Lets enterprise clients review their own past and active orders and cancel orders that have not yet entered processing.

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

### Requirement: Client dashboard layout
The system SHALL show the Enterprise Client an "Active Procurement Tracker" with a count pill and one card per non-terminal order (id, date, units, net total, stepper, view link, and a cancel button only when cancellation is allowed), followed by an "Order History Log" table of `DELIVERED` and `CANCELLED` orders, as in `figma-designs/enterprise-user-dashboard.html`. A "+ Create New Order" action SHALL open the order form.

#### Scenario: Active and history split
- **WHEN** a client has orders in several statuses
- **THEN** non-terminal orders appear in the tracker and terminal orders in the history table

#### Scenario: No active orders
- **WHEN** the client has no non-terminal orders
- **THEN** the tracker shows an empty-state message

### Requirement: Cancel confirmation
The system SHALL ask for confirmation in a modal ("Confirm Cancellation" with "Keep Order" and "Yes, Cancel Request") before cancelling, and SHALL show a toast after a successful cancellation.

#### Scenario: Keep order
- **WHEN** the client opens the cancel modal and chooses "Keep Order"
- **THEN** the modal closes and the order is unchanged
