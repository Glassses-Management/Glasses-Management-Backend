-- Make scheduled_at nullable for exam requests (appointment is created before scheduling)
ALTER TABLE appointment ALTER COLUMN scheduled_at DROP NOT NULL;
