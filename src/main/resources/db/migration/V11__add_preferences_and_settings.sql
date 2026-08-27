-- V11: Member preferences (per-member) + Guild global settings.
--
-- These are deliberately SEPARATE tables (research-backed): member preferences
-- belong to one member and are edited by that member; guild settings belong to
-- the Guild as a whole and are edited only by admins. They never share a table.
--
-- Member preferences: typed columns, one row per member (keyed by user_id).
-- Guild settings: a singleton row (id = 1) for the whole installation.

CREATE TABLE IF NOT EXISTS member_preferences (
    user_id UUID PRIMARY KEY REFERENCES users(id),
    email_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    weekly_digest BOOLEAN NOT NULL DEFAULT FALSE,
    new_article_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    comment_alerts BOOLEAN NOT NULL DEFAULT TRUE,
    member_announcements BOOLEAN NOT NULL DEFAULT FALSE,
    dark_mode BOOLEAN NOT NULL DEFAULT FALSE,
    font_size VARCHAR(20) NOT NULL DEFAULT 'medium',
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS guild_settings (
    id BIGINT PRIMARY KEY,
    registration_open BOOLEAN NOT NULL DEFAULT TRUE,
    portal_name VARCHAR(120) NOT NULL DEFAULT 'GNS Members Portal',
    accent_color VARCHAR(20) NOT NULL DEFAULT '#111418',
    footer_text VARCHAR(500) NOT NULL DEFAULT '© 2026 Guild of Nigerian Speechwriters. All rights reserved.',
    guild_name VARCHAR(120) NOT NULL DEFAULT 'Guild of Nigerian Speechwriters',
    contact_email VARCHAR(255),
    contact_phone VARCHAR(40),
    address VARCHAR(255),
    id_prefix VARCHAR(20) NOT NULL DEFAULT 'GNS',
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);