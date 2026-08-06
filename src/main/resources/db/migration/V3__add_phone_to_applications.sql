-- Add phone column to applications table
ALTER TABLE applications ADD COLUMN IF NOT EXISTS phone VARCHAR(255);