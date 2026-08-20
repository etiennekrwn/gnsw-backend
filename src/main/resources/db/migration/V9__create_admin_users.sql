-- V9: Fully separate admin accounts, decoupled from the member "users" table.
-- Admins have their own role, an optional module permission set (MANAGER),
-- and an email-invite lifecycle (token + expiry).
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