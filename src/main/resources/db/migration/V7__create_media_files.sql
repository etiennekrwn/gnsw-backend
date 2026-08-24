-- Media files library for the admin portal
CREATE TABLE IF NOT EXISTS media_files (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    file_name       VARCHAR(255) NOT NULL,
    original_name   VARCHAR(255),
    file_type       VARCHAR(100),
    content_type    VARCHAR(100),
    file_size       BIGINT,
    file_url        TEXT,
    thumbnail_url   TEXT,
    uploader_id     UUID,
    uploader_role   VARCHAR(50),
    is_image        BOOLEAN,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_media_files_uploader ON media_files(uploader_id);
CREATE INDEX IF NOT EXISTS idx_media_files_image ON media_files(is_image);
CREATE INDEX IF NOT EXISTS idx_media_files_created ON media_files(created_at DESC);