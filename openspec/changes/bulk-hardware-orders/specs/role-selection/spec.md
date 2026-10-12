# Spec Delta

## Purpose

Lets a demo user choose the active role, and the client for the Enterprise Client role, without authentication, so each role's views and actions can be exercised.

## ADDED Requirements

### Requirement: Role selector
The system SHALL provide a dropdown to select the active role from: Enterprise Client, Warehouse Operator, and Invoice Department. The system SHALL NOT require authentication.

#### Scenario: Roles offered
- **WHEN** the user opens the role dropdown
- **THEN** exactly the options Enterprise Client, Warehouse Operator, and Invoice Department are listed

#### Scenario: No login required
- **WHEN** the user opens the application
- **THEN** a role can be selected without entering credentials

### Requirement: Client selector for Enterprise Client
When the Enterprise Client role is active, the system SHALL show a second dropdown listing the seeded clients, and SHALL treat the selected client as the acting client for all order intake, history, and cancellation.

#### Scenario: Client dropdown shown
- **WHEN** the Enterprise Client role is selected
- **THEN** a dropdown listing all seeded clients is displayed

#### Scenario: Client dropdown hidden for other roles
- **WHEN** the Warehouse Operator or Invoice Department role is selected
- **THEN** no client dropdown is displayed

#### Scenario: Acting client scopes the view
- **WHEN** the user switches the selected client
- **THEN** order history and new orders apply to the newly selected client only

### Requirement: Role capabilities
The system SHALL expose only the actions of the active role: Enterprise Client may place orders, view own history, and cancel eligible orders; Warehouse Operator may advance order status; Invoice Department may only view all orders.

#### Scenario: Invoice Department is read-only
- **WHEN** the Invoice Department role is active
- **THEN** all orders are listed and no action to place, cancel, or advance an order is offered

#### Scenario: Warehouse Operator cannot place orders
- **WHEN** the Warehouse Operator role is active
- **THEN** no action to place or cancel an order is offered

### Requirement: Seeded reference data
The system SHALL provide seeded mock data standing in for an external system: clients with a flat contract discount percentage, and products with SKU and unit price.

#### Scenario: Seed data available at startup
- **WHEN** the application starts for the first time
- **THEN** at least one client with a discount percentage and at least one product with a unit price are available

### Requirement: Role-aware page header
The system SHALL show a header on every view containing the app title, the active role's portal label, the Role dropdown, and a contract badge showing the acting client's discount for the Enterprise Client role. The header icon SHALL be blue for Enterprise Client, dark slate for Warehouse Operator, and purple for Invoice Department, matching `figma-designs/`.

#### Scenario: Header follows role
- **WHEN** the user switches role
- **THEN** the portal label and icon color change to that role's, and the contract badge is shown only for Enterprise Client
