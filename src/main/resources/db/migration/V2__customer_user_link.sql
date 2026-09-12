-- Link a self-registered customer profile to its login account (User, role CUSTOMER).
ALTER TABLE customer
    ADD COLUMN user_id BIGINT,
    ADD CONSTRAINT fk_customer_user FOREIGN KEY (user_id) REFERENCES `user` (id);
