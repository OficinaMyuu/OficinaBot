-- +goose Up
CREATE TABLE admin_sessions (
    token_hash TEXT PRIMARY KEY,
    discord_id TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at DATETIME NOT NULL,
    FOREIGN KEY (discord_id) REFERENCES users(discord_id) ON DELETE CASCADE
);

CREATE INDEX idx_admin_sessions_discord_id ON admin_sessions(discord_id);
CREATE INDEX idx_admin_sessions_expires_at ON admin_sessions(expires_at);

-- +goose Down
DROP TABLE admin_sessions;
