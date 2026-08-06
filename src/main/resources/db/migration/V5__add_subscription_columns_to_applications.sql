-- Add subscription columns to applications so the Paystack subscription
-- (created at checkout) can be captured before the user is approved.
ALTER TABLE applications
    ADD COLUMN IF NOT EXISTS subscription_code VARCHAR(255),
    ADD COLUMN IF NOT EXISTS plan_code VARCHAR(255),
    ADD COLUMN IF NOT EXISTS email_token VARCHAR(255);