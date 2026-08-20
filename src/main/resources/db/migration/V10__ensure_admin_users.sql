-- V10: Guarantee the admin_users table exists in the deployed database.
-- The original V9 migration that was applied to production did not actually
-- create this table (its recorded checksum differed, and because the schema
-- was already "at version 9", Flyway never re-ran it) -- which caused Hibernate
-- schema-validation to fail with "missing table [admin_users]".
--
-- This is idempotent: it creates the table if absent and adds any missing
-- columns, so it is safe on both broken and already-correct databases.
CREATE TABLE IF NOT EXISTS admin_users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    display_name VARCHAR(120) NOT NULL,
    password_hash VARCHAR(255),
    role VARCHAR(30) NOT NULL,
    allowed_modules TEXT,
    status VARCHAR(20) NOT NULL,
    invite_token_hash VARCHAR(64),
    invite_token_expires_at TIMESTAMP,
    invited_by UUID,
    invited_at TIMESTAMP,
    activated_at TIMESTAMP,
    last_login_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Column-level safety for databases that partially created the table.
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS email VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS display_name VARCHAR(120) NOT NULL DEFAULT '';
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS role VARCHAR(30) NOT NULL DEFAULT 'MANAGER';
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS allowed_modules TEXT;
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'INVITED';
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS invite_token_hash VARCHAR(64);
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS invite_token_expires_at TIMESTAMP;
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS invited_by UUID;
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS invited_at TIMESTAMP;
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS activated_at TIMESTAMP;
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP;
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE admin_users ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();

-- Ensure the unique constraint on email exists (needed by AdminUserRepository).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'admin_users_email_key'
    ) THEN
        ALTER TABLE admin_users ADD CONSTRAINT admin_users_email_key UNIQUE (email);
    END IF;
END $$;