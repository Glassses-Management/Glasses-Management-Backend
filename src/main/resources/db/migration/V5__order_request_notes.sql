-- Support customer-submitted glasses requests stored as orders in PENDING_REVIEW.
ALTER TABLE orders
    ADD COLUMN request_type VARCHAR(20),
    ADD COLUMN notes TEXT;
