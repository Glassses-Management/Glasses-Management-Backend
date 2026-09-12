-- Run this SQL against your MySQL database to fix stale columns.
-- These columns exist in the DB but were removed from the Java entities.

-- 1. Drop product_id from orders (was from old schema, NOT NULL without default = causes 500 error)
    ALTER TABLE orders DROP COLUMN product_id;

-- 2. Drop branch_id from all tables (Branch feature removed)
ALTER TABLE orders DROP COLUMN branch_id;
ALTER TABLE inventory DROP COLUMN branch_id;
ALTER TABLE `user` DROP COLUMN branch_id;
ALTER TABLE appointment DROP COLUMN branch_id;
ALTER TABLE prescription DROP COLUMN branch_id;

-- 3. Drop the branch table
DROP TABLE IF EXISTS branch;

-- 4. Change money columns from FLOAT to DECIMAL for precision
ALTER TABLE orders MODIFY COLUMN total DECIMAL(19,2);
ALTER TABLE order_item MODIFY COLUMN len_price DECIMAL(19,2);
ALTER TABLE order_item MODIFY COLUMN unit_price DECIMAL(19,2);
ALTER TABLE order_item MODIFY COLUMN total_price DECIMAL(19,2);
