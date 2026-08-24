-- Members community feed articles (the "social media" part of the portal).
CREATE TABLE IF NOT EXISTS articles (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    author_id      UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title          VARCHAR(255) NOT NULL,
    excerpt        TEXT,
    content        TEXT,          -- JSON array of rich-text blocks OR HTML (Tiptap)
    tags           TEXT,          -- JSON array of keyword strings
    thumbnail_url  TEXT,
    image_url      TEXT,
    tag            VARCHAR(60),   -- 'For You' | 'Featured' | 'Latest' | 'Trending'
    category       VARCHAR(120),
    read_time      INTEGER,
    claps          INTEGER DEFAULT 0,
    views          BIGINT DEFAULT 0,
    comment_count  INTEGER DEFAULT 0,
    featured       BOOLEAN DEFAULT FALSE,
    status         VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    published_at   TIMESTAMP,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_articles_status_published
    ON articles(status, published_at DESC);
CREATE INDEX IF NOT EXISTS idx_articles_author
    ON articles(author_id);