-- V1: Initial GNS schema.
-- Allows fresh deployments to bootstrap the full database (the app is configured
-- with ddl-auto=validate, so the schema must exist before startup).
-- Every statement is guarded with IF NOT EXISTS so this migration is a safe
-- no-op on databases that already contain the schema (e.g. the original Railway
-- DB that was created outside Flyway and baselined at V1).

CREATE TABLE IF NOT EXISTS users (
    id                               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email                            VARCHAR(255) NOT NULL UNIQUE,
    first_name                       VARCHAR(255) NOT NULL,
    last_name                        VARCHAR(255) NOT NULL,
    address_line1                    VARCHAR(255) NOT NULL,
    address_line2                    VARCHAR(255),
    city                             VARCHAR(255) NOT NULL,
    state_province                   VARCHAR(255) NOT NULL,
    zip_postal_code                  VARCHAR(255) NOT NULL,
    country                          VARCHAR(255) NOT NULL,
    username                         VARCHAR(255) UNIQUE,
    password_hash                    VARCHAR(255),
    tier                             VARCHAR(255) NOT NULL,
    status                           VARCHAR(255) NOT NULL,
    role                             VARCHAR(255) NOT NULL,
    professional_id                  VARCHAR(255) UNIQUE,
    email_verified_at                TIMESTAMP,
    approved_at                      TIMESTAMP,
    approved_by                      UUID,
    rejected_at                      TIMESTAMP,
    rejected_by                      UUID,
    rejection_reason                 VARCHAR(255),
    password_set_token               VARCHAR(255),
    password_set_token_expires_at    TIMESTAMP,
    password_set_at                  TIMESTAMP,
    password_reset_token             VARCHAR(255),
    password_reset_token_expires_at  TIMESTAMP,
    activation_reminder_count        INTEGER NOT NULL DEFAULT 0,
    last_login_at                    TIMESTAMP,
    created_at                       TIMESTAMP NOT NULL,
    updated_at                       TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS members (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id            UUID NOT NULL UNIQUE,
    profile_image_url  VARCHAR(255),
    organisation       VARCHAR(255),
    bio                TEXT,
    phone              VARCHAR(255),
    sectors            TEXT,
    speech_types       TEXT,
    languages          TEXT,
    zone               VARCHAR(255),
    linkedin_profile   VARCHAR(500),
    socials            TEXT,
    reason_for_joining TEXT,
    is_verified        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP NOT NULL,
    updated_at         TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS applications (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name         VARCHAR(255) NOT NULL,
    last_name          VARCHAR(255) NOT NULL,
    email              VARCHAR(255) NOT NULL UNIQUE,
    address_line1      VARCHAR(255) NOT NULL,
    address_line2      VARCHAR(255),
    city               VARCHAR(255) NOT NULL,
    state_province     VARCHAR(255) NOT NULL,
    zip_postal_code    VARCHAR(255) NOT NULL,
    country            VARCHAR(255) NOT NULL,
    membership_tier    VARCHAR(255) NOT NULL,
    status             VARCHAR(255) NOT NULL,
    phone              VARCHAR(255),
    linkedin_profile   VARCHAR(500),
    socials            TEXT,
    bio                TEXT,
    reason_for_joining TEXT,
    sectors            TEXT,
    speech_types       TEXT,
    languages          TEXT,
    payment_reference  VARCHAR(255) UNIQUE,
    payment_amount     INTEGER,
    payment_status     VARCHAR(255),
    subscription_code  VARCHAR(255),
    plan_code          VARCHAR(255),
    email_token        VARCHAR(255),
    email_verified     BOOLEAN NOT NULL DEFAULT FALSE,
    reviewed_at        TIMESTAMP,
    reviewed_by        UUID,
    rejection_reason   VARCHAR(255),
    created_at         TIMESTAMP NOT NULL,
    updated_at         TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS payments (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID,
    reference         VARCHAR(255) NOT NULL UNIQUE,
    amount            INTEGER NOT NULL,
    tier              VARCHAR(255) NOT NULL,
    status            VARCHAR(255) NOT NULL,
    paystack_response TEXT,
    paid_at           TIMESTAMP,
    created_at        TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS member_subscriptions (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL,
    subscription_code VARCHAR(255) UNIQUE,
    plan_code         VARCHAR(255) NOT NULL,
    email_token       VARCHAR(255),
    status            VARCHAR(255) NOT NULL,
    next_payment_date TIMESTAMP,
    created_at        TIMESTAMP NOT NULL,
    updated_at        TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS email_otps (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email       VARCHAR(255) NOT NULL,
    otp_code    VARCHAR(6) NOT NULL,
    expires_at  TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    attempts    INTEGER NOT NULL DEFAULT 0,
    created_at  TIMESTAMP NOT NULL
);
