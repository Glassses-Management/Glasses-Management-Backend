-- Make scheduled_at nullable for exam requests (appointment is created before scheduling)
ALTER TABLE appointment MODIFY COLUMN scheduled_at DATETIME NULL;
