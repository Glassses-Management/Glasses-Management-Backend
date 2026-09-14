-- Create Request table for the optical shop request workflow.
-- A Request represents a customer request (exam or product) that staff must review.
-- Product purchase requests map to Orders; exam requests can lead to Appointments.

CREATE TABLE request (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    notes TEXT,
    prescription_id BIGINT,
    created_at DATETIME,
    updated_at DATETIME,
    CONSTRAINT fk_request_customer FOREIGN KEY (customer_id) REFERENCES customer (id),
    CONSTRAINT fk_request_prescription FOREIGN KEY (prescription_id) REFERENCES prescription (id)
);

-- Add request_id to appointment so an Appointment can reference the Request that created it.
-- Relationship: Request 1 → 0..1 Appointment
ALTER TABLE appointment ADD COLUMN request_id BIGINT;
ALTER TABLE appointment ADD CONSTRAINT fk_appointment_request FOREIGN KEY (request_id) REFERENCES request (id);
