-- history is always read in chronological order per order
CREATE INDEX idx_history_order_changed ON order_status_history (order_id, changed_at, id);
