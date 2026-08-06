-- Make payments.user_id nullable since a payment can be recorded
-- before a User is created (application is created first, user on approval).
ALTER TABLE payments ALTER COLUMN user_id DROP NOT NULL;