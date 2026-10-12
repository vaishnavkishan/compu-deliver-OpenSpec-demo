CREATE TABLE order_edits (
    id        bigserial PRIMARY KEY,
    order_id  bigint NOT NULL REFERENCES orders (id),
    edited_at timestamptz NOT NULL
);
CREATE INDEX idx_edits_order ON order_edits (order_id);
