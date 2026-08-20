-- V12: Admin-console per-admin light/dark theme, and member account-deletion
-- request tracking.
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS theme VARCHAR(10) NOT NULL DEFAULT 'LIGHT';

ALTER TABLE users ADD COLUMN IF NOT EXISTS deletion_requested_at TIMESTAMP;