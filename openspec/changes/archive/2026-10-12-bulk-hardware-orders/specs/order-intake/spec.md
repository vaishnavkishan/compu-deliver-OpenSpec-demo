# Spec Delta

## Purpose

Accepts bulk hardware orders from enterprise clients and computes the net total using the client's pre-negotiated flat contract discount, preserving the pricing as a snapshot on the order.

## ADDED Requirements

### Requirement: Place order
The system SHALL allow an Enterprise Client to submit an order consisting of one or more lines, each with a product SKU and a positive integer quantity. A newly accepted order SHALL have status `SUBMITTED`.

#### Scenario: Successful order
- **WHEN** a client submits an order with valid SKUs and positive quantities
- **THEN** the order is created with status `SUBMITTED` and associated with that client

#### Scenario: Empty order rejected
- **WHEN** a client submits an order with no lines
- **THEN** the order is rejected and no order is created

#### Scenario: Invalid quantity rejected
- **WHEN** a line has a quantity that is zero, negative, or not a whole number
- **THEN** the order is rejected with an error identifying the line

#### Scenario: Unknown SKU rejected
- **WHEN** a line references a SKU that does not exist
- **THEN** the order is rejected with an error identifying the SKU

### Requirement: Net total from contract discount
The system SHALL compute gross as the sum of unit price times quantity over all lines, apply the client's flat contract discount percentage, and compute net as gross minus the discount. Monetary amounts SHALL be calculated without floating-point rounding error and rounded to the smallest currency unit.

#### Scenario: Discount applied
- **WHEN** a client with a 10% discount orders 10 units at 100.00 each
- **THEN** gross is 1000.00, discount is 100.00, and net is 900.00

#### Scenario: Zero discount contract
- **WHEN** a client with a 0% discount places an order
- **THEN** net equals gross

#### Scenario: Rounding to smallest unit
- **WHEN** the discount produces a fractional currency unit
- **THEN** the amount is rounded to the smallest currency unit and net equals gross minus the rounded discount

### Requirement: Reject orders without a contract
The system SHALL reject an order from a client that has no contract discount on record and SHALL NOT default to full price.

#### Scenario: No contract
- **WHEN** a client with no contract submits an order
- **THEN** the order is rejected with an error stating no contract exists and no order is created

### Requirement: Pricing snapshot
The system SHALL store the gross, discount percentage, and net on the order at intake, and SHALL NOT change them when client contracts or product prices change afterward. The only exception is an edit of a `SUBMITTED` order (see `order-edit`), which replaces the snapshot.

#### Scenario: Contract changes after order
- **WHEN** a client's discount percentage changes after an order was placed
- **THEN** the existing order still shows its original gross, discount percentage, and net

### Requirement: Order form layout
The system SHALL present the new-order page as in `figma-designs/enterprise-user-detail-page.html`: an "Order Line Items" table where each row has a searchable product picker (matching SKU or name, with a "No matching products found" state), unit price, a quantity input with minimum 1, a subtotal, and a remove control, plus buttons to add a line. A sticky "Contract Net Pricing" panel SHALL show total units, gross, contract discount with its percentage, "Logistics & Freight: Waived", and the net total, updating as lines change.

#### Scenario: Search and select product
- **WHEN** the user types part of a SKU or product name in a line's picker
- **THEN** matching products are listed with name, SKU, and price, and choosing one fills the row

#### Scenario: Totals update live
- **WHEN** the user changes a quantity or removes a line
- **THEN** the line subtotal and the pricing panel update immediately

#### Scenario: Submit feedback
- **WHEN** an order is submitted, or rejected with an error
- **THEN** a toast at bottom-right shows the outcome and fades after about 3 seconds
