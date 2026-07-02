-- +goose Up
CREATE TABLE admin_sessions (
    token_hash VARCHAR(128) PRIMARY KEY,
    discord_id VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_seen_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    expires_at DATETIME(6) NOT NULL,
    FOREIGN KEY (discord_id) REFERENCES admin_users(discord_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_admin_sessions_discord_id ON admin_sessions(discord_id);
CREATE INDEX idx_admin_sessions_expires_at ON admin_sessions(expires_at);

-- +goose Down
DROP TABLE admin_sessions;
