-- V4: Add professional fields to applications and members tables

-- Add professional fields to applications (captured at apply time)
ALTER TABLE applications
    ADD COLUMN IF NOT EXISTS linkedin_profile VARCHAR(500),
    ADD COLUMN IF NOT EXISTS socials          TEXT,
    ADD COLUMN IF NOT EXISTS bio              TEXT,
    ADD COLUMN IF NOT EXISTS reason_for_joining TEXT,
    ADD COLUMN IF NOT EXISTS sectors          TEXT,
    ADD COLUMN IF NOT EXISTS speech_types     TEXT,
    ADD COLUMN IF NOT EXISTS languages        TEXT;

-- Add linkedin, socials, and reason_for_joining to members
-- (bio, sectors, speech_types, languages already exist on members)
ALTER TABLE members
    ADD COLUMN IF NOT EXISTS linkedin_profile    VARCHAR(500),
    ADD COLUMN IF NOT EXISTS socials             TEXT,
    ADD COLUMN IF NOT EXISTS reason_for_joining  TEXT;
