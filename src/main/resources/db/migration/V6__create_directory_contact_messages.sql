-- Contact messages submitted from the public members directory
-- (the "contact member" modal on the main website)
CREATE TABLE IF NOT EXISTS directory_contact_messages (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    member_id   UUID NOT NULL REFERENCES users(id),
    sender_name VARCHAR(255) NOT NULL,
    sender_email VARCHAR(255) NOT NULL,
    message     TEXT NOT NULL,
    is_read     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_directory_contact_messages_member
    ON directory_contact_messages(member_id);
CREATE INDEX IF NOT EXISTS idx_directory_contact_messages_read
    ON directory_contact_messages(is_read);