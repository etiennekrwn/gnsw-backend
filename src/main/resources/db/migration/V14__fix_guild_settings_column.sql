-- V14: Fix GuildSetting entity/column name mismatch.
-- The GuildSetting entity maps to registration_enabled, but V11 created
-- the column as registration_open. Prod DB is already past V11, so the column
-- never exists and Hibernate schema-validation crashes the app on startup.
ALTER TABLE guild_settings ADD COLUMN IF NOT EXISTS registration_enabled BOOLEAN NOT NULL DEFAULT TRUE;
UPDATE guild_settings SET registration_enabled = registration_open WHERE registration_open IS NOT NULL;
ALTER TABLE guild_settings DROP COLUMN IF EXISTS registration_open;
