-- V13: Subscription grace period + dunning tracking on member_subscriptions.
--
-- A member whose renewal charge fails enters a configurable grace window
-- (default 5 days, SUBSCRIPTION_GRACE_DAYS). grace_started_at records when the
-- first failed attempt happened so a nightly job can transition past_due ->
-- expired once the grace window elapses. last_payment_attempt_at is the most
-- recent renewal attempt, so dunning emails can be throttled.
ALTER TABLE member_subscriptions ADD COLUMN IF NOT EXISTS grace_started_at TIMESTAMP;
ALTER TABLE member_subscriptions ADD COLUMN IF NOT EXISTS last_payment_attempt_at TIMESTAMP;