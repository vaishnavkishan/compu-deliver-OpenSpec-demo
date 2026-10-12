# Spec Delta

## Purpose

Lets an enterprise client correct an order's line items while it is still at the Intake step (`SUBMITTED`), before the warehouse has started working on it.

## ADDED Requirements

### Requirement: Edit order while SUBMITTED
The system SHALL allow an Enterprise Client to replace the lines of their own order only while its status is `SUBMITTED`. The edit SHALL use the same validation as placing an order (at least one line, positive integer quantities, known SKUs, client has a contract). The order's status SHALL NOT change.

#### Scenario: Successful edit
- **WHEN** a client edits their `SUBMITTED` order with valid lines
- **THEN** the order's lines are replaced and the order remains `SUBMITTED`

#### Scenario: Edit rejected after intake step
- **WHEN** a client edits an order that is `CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED` or `CANCELLED`
- **THEN** the edit is rejected with a conflict error and the order is unchanged

#### Scenario: Another client's order
- **WHEN** a client edits an order belonging to a different client
- **THEN** the edit is rejected and the order is unchanged

#### Scenario: Non-client role
- **WHEN** a Warehouse Operator attempts to edit an order
- **THEN** the edit is rejected

#### Scenario: Invalid edit rejected
- **WHEN** an edit has no lines, a non-positive or non-integer quantity, an unknown SKU, or the client has no contract
- **THEN** the edit is rejected with an error and the order is unchanged

#### Scenario: Edit races with advance or cancel
- **WHEN** an edit and an advance or cancel are applied concurrently to the same order
- **THEN** exactly one succeeds and the other is rejected with a conflict error

### Requirement: Re-pricing on edit
The system SHALL recompute gross, discount percentage and net on edit using the client's current contract discount and current product prices, and SHALL replace the order's pricing snapshot and line unit prices with the result.

#### Scenario: Totals recomputed
- **WHEN** a client with a 10% discount edits an order to 5 units at 100.00 each
- **THEN** gross is 500.00, discount is 50.00, and net is 450.00

### Requirement: Edit history
The system SHALL record a timestamp for every edit and SHALL show it with the order.

#### Scenario: Edit timestamp recorded
- **WHEN** a client edits a `SUBMITTED` order
- **THEN** the order's history shows an edit entry with the time of the change

### Requirement: Edit order UI
The order page SHALL show an "Edit Order" button only for a `SUBMITTED` order. It SHALL make the form editable, prefilled with the current lines, with a "Save Changes" button; the button SHALL NOT appear for any other status.

#### Scenario: Edit button visibility
- **WHEN** a client views an order
- **THEN** "Edit Order" is shown only if the order is `SUBMITTED`
