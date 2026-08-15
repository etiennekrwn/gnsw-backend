-- Password reset flow for members who forget their password
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS password_reset_token          VARCHAR(255),
    ADD COLUMN IF NOT EXISTS password_reset_token_expires_at TIMESTAMP;

-- Bounded automatic activation-link reminders for accepted members
-- who never set their password before the original link expired.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS activation_reminder_count     INTEGER NOT NULL DEFAULT 0;

CREATE INDEX IF NOT EXISTS idx_users_password_reset_token
    ON users(password_reset_token);