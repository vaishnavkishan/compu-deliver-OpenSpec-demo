CREATE TABLE clients (
    id          bigint PRIMARY KEY,
    name        varchar(120) NOT NULL,
    discount_bp integer CHECK (discount_bp BETWEEN 0 AND 10000)
);

CREATE TABLE products (
    sku              varchar(40) PRIMARY KEY,
    name             varchar(160) NOT NULL,
    unit_price_cents bigint NOT NULL CHECK (unit_price_cents >= 0)
);

CREATE TABLE orders (
    id          bigserial PRIMARY KEY,
    client_id   bigint NOT NULL REFERENCES clients (id),
    status      varchar(20) NOT NULL,
    gross_cents bigint NOT NULL,
    discount_bp integer NOT NULL,
    net_cents   bigint NOT NULL,
    created_at  timestamptz NOT NULL,
    version     bigint NOT NULL DEFAULT 0
);
CREATE INDEX idx_orders_client ON orders (client_id);

CREATE TABLE order_lines (
    id               bigserial PRIMARY KEY,
    order_id         bigint NOT NULL REFERENCES orders (id),
    sku              varchar(40) NOT NULL REFERENCES products (sku),
    qty              integer NOT NULL CHECK (qty > 0),
    unit_price_cents bigint NOT NULL
);
CREATE INDEX idx_order_lines_order ON order_lines (order_id);

CREATE TABLE order_status_history (
    id         bigserial PRIMARY KEY,
    order_id   bigint NOT NULL REFERENCES orders (id),
    status     varchar(20) NOT NULL,
    changed_at timestamptz NOT NULL
);
CREATE INDEX idx_history_order ON order_status_history (order_id);
