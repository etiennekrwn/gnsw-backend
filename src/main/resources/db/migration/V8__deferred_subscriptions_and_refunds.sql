-- V8: Deferred subscription billing + manual refunds.
--
-- authorization_code: card authorization captured at checkout. The Paystack
-- subscription is created ONLY at approval (POST /subscription with
-- start_date = approval + 1 year), so no subscription exists while the
-- application is pending review and rejected applicants are never charged
-- again. This column stores the authorization needed to create it later.
ALTER TABLE applications ADD COLUMN IF NOT EXISTS authorization_code VARCHAR(255);

-- Refund tracking on payments (idempotent manual refunds from the admin).
ALTER TABLE payments ADD COLUMN IF NOT EXISTS refund_status VARCHAR(255) DEFAULT 'NONE';
ALTER TABLE payments ADD COLUMN IF NOT EXISTS refund_reference VARCHAR(255);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS refunded_at TIMESTAMP;
