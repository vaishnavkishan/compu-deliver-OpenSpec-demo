# Spec Delta

## Purpose

Tracks each order from intake to final delivery through a fixed set of statuses, with a recorded history of transitions. The Warehouse Operator advances orders.

## ADDED Requirements

### Requirement: Order statuses
The system SHALL track each order with exactly one status from: `SUBMITTED`, `CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`. `DELIVERED` and `CANCELLED` SHALL be terminal.

#### Scenario: Terminal states
- **WHEN** an order is `DELIVERED` or `CANCELLED`
- **THEN** no further status change is permitted

### Requirement: Warehouse Operator advances status
The system SHALL allow a Warehouse Operator to advance an order forward exactly one step along `SUBMITTED > CONFIRMED > PROCESSING > SHIPPED > DELIVERED`, through an advance action in the portal's Warehouse Operator view. Other roles SHALL NOT be able to advance orders.

#### Scenario: Valid advance
- **WHEN** a Warehouse Operator advances a `CONFIRMED` order
- **THEN** the order status becomes `PROCESSING`

#### Scenario: Skipping a step rejected
- **WHEN** a request tries to move a `SUBMITTED` order directly to `SHIPPED`
- **THEN** the change is rejected and the status is unchanged

#### Scenario: Backward move rejected
- **WHEN** a request tries to move a `SHIPPED` order back to `PROCESSING`
- **THEN** the change is rejected and the status is unchanged

#### Scenario: Advance past terminal rejected
- **WHEN** a Warehouse Operator tries to advance a `DELIVERED` or `CANCELLED` order
- **THEN** the change is rejected and the status is unchanged

#### Scenario: Non-operator rejected
- **WHEN** an Enterprise Client sends an advance request
- **THEN** the request is rejected and the status is unchanged

### Requirement: Warehouse Operator cannot cancel
The system SHALL NOT allow a Warehouse Operator to cancel an order.

#### Scenario: No cancel for operator
- **WHEN** the Warehouse Operator role is active
- **THEN** no cancel action is available and a cancel request is rejected

### Requirement: Concurrent changes
The system SHALL use optimistic locking on orders so that concurrent status changes on one order result in one success and one conflict error.

#### Scenario: Cancel races advance
- **WHEN** a cancel and an advance are applied concurrently to the same order
- **THEN** exactly one succeeds and the other is rejected with a conflict error

### Requirement: Transition history
The system SHALL record a timestamp for every status transition, including the initial `SUBMITTED` status and cancellation, and SHALL make this history visible with the order.

#### Scenario: Timestamp recorded
- **WHEN** an order moves from `CONFIRMED` to `PROCESSING`
- **THEN** the order's history contains a `PROCESSING` entry with the time of the change

#### Scenario: Initial entry
- **WHEN** an order is placed
- **THEN** its history contains a `SUBMITTED` entry with the time of intake

### Requirement: Lifecycle stepper and status badges
The system SHALL display each order's progress as a five-step numbered stepper (Submitted, Confirmed, Processing, Shipped, Delivered) in which completed steps are green, the current step is blue, and later steps are grey. Status badges SHALL be colored: Submitted blue, Confirmed sky, Processing amber, Shipped indigo, Delivered green, Cancelled red. A `CANCELLED` order SHALL show a red notice in place of the stepper.

#### Scenario: Stepper reflects status
- **WHEN** an order is `PROCESSING`
- **THEN** steps Submitted and Confirmed are green, Processing is blue, and Shipped and Delivered are grey

#### Scenario: Cancelled order display
- **WHEN** an order is `CANCELLED`
- **THEN** a red "cancelled" notice replaces the stepper
