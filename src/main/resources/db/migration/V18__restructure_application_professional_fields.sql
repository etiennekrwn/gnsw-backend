-- V18: Restructure application professional fields
-- Replaces sectors/speech_types/languages with role/orator fields, folds
-- LinkedIn into a single social-media field, and adds training/qualification fields.

-- ---------- applications ----------
-- Remove obsolete columns
ALTER TABLE applications
    DROP COLUMN IF EXISTS linkedin_profile,
    DROP COLUMN IF EXISTS sectors,
    DROP COLUMN IF EXISTS speech_types,
    DROP COLUMN IF EXISTS languages;

-- Repurpose socials -> social_media_platform
ALTER TABLE applications RENAME COLUMN socials TO social_media_platform;

-- Add new professional fields
ALTER TABLE applications
    ADD COLUMN IF NOT EXISTS current_professional_role VARCHAR(200),
    ADD COLUMN IF NOT EXISTS favourite_orator       VARCHAR(300),
    ADD COLUMN IF NOT EXISTS speechwriting_training VARCHAR(10),
    ADD COLUMN IF NOT EXISTS training_details       TEXT,
    ADD COLUMN IF NOT EXISTS highest_qualification  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS current_job_title      VARCHAR(200),
    ADD COLUMN IF NOT EXISTS current_organization   VARCHAR(200);

-- ---------- members ----------
ALTER TABLE members
    DROP COLUMN IF EXISTS linkedin_profile,
    DROP COLUMN IF EXISTS sectors,
    DROP COLUMN IF EXISTS speech_types,
    DROP COLUMN IF EXISTS languages;

ALTER TABLE members RENAME COLUMN socials TO social_media_platform;

ALTER TABLE members
    ADD COLUMN IF NOT EXISTS current_professional_role VARCHAR(200),
    ADD COLUMN IF NOT EXISTS favourite_orator       VARCHAR(300),
    ADD COLUMN IF NOT EXISTS speechwriting_training VARCHAR(10),
    ADD COLUMN IF NOT EXISTS training_details       TEXT,
    ADD COLUMN IF NOT EXISTS highest_qualification  VARCHAR(100),
    ADD COLUMN IF NOT EXISTS current_job_title      VARCHAR(200),
    ADD COLUMN IF NOT EXISTS current_organization   VARCHAR(200);