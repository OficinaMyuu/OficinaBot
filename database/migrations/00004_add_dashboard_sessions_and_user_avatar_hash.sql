-- +goose Up
ALTER TABLE users
    ADD COLUMN avatar_hash VARCHAR(128);

CREATE TABLE dashboard_sessions (
    session_id_hash CHAR(64) PRIMARY KEY,
    csrf_token VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL,
    username VARCHAR(255) NOT NULL,
    global_name VARCHAR(255),
    avatar_url TEXT,
    guild_name VARCHAR(255) NOT NULL,
    guild_icon_url TEXT,
    permissions VARCHAR(64) NOT NULL,
    expires_at BIGINT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    INDEX idx_dashboard_sessions_expires_at (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- +goose Down
DROP TABLE dashboard_sessions;

ALTER TABLE users
    DROP COLUMN avatar_hash;
