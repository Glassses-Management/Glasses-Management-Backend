-- ============================================================================
-- OBSOLETE — this script is MySQL-only and no longer applies.
--
-- The project has moved to PostgreSQL on Aiven (see docs/AIVEN_SETUP.md). The
-- schema is now owned entirely by Flyway (src/main/resources/db/migration/),
-- Hibernate runs with ddl-auto=validate, and these repairs have already been
-- folded into the migration history.
--
-- Do not run this against the PostgreSQL database: the syntax is invalid there
-- (MODIFY COLUMN, backticks) and it would fail on the first statement.
-- ============================================================================

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
