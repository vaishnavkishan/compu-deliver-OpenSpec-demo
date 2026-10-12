# Spec Delta

## Purpose

Announces accepted orders to the rest of the platform by publishing an `OrderIntaken` event to RabbitMQ, so future Warehouse and Invoicing services can react without coupling to the Order API.

## ADDED Requirements

### Requirement: Publish OrderIntaken
The system SHALL publish an `OrderIntaken` event to RabbitMQ exactly once for each order that is successfully accepted and committed. The event SHALL contain the order id, client id, lines (SKU and quantity), gross, discount percentage, net, and the intake timestamp.

#### Scenario: Event on successful intake
- **WHEN** a client's order is accepted
- **THEN** one `OrderIntaken` message with that order's id, client id, lines, and pricing snapshot is published

#### Scenario: No event for rejected order
- **WHEN** an order is rejected (invalid input, unknown SKU, or no contract)
- **THEN** no `OrderIntaken` message is published

### Requirement: Publish after commit
The system SHALL publish the event only after the order transaction has committed.

#### Scenario: Rollback emits nothing
- **WHEN** the order transaction rolls back after the order was built
- **THEN** no `OrderIntaken` message is published
