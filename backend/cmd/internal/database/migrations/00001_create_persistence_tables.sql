-- +goose Up
CREATE TABLE users (
    discord_id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE bot_clients (
    name VARCHAR(128) PRIMARY KEY,
    token_hash VARCHAR(128) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_seen_at DATETIME(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE event_batches (
    id VARCHAR(128) PRIMARY KEY,
    client_name VARCHAR(128) NOT NULL,
    kind VARCHAR(64) NOT NULL,
    received_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (client_name) REFERENCES bot_clients(name) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE message_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id VARCHAR(128) NOT NULL,
    guild_id VARCHAR(64) NOT NULL,
    channel_id VARCHAR(64) NOT NULL,
    message_id VARCHAR(64) NOT NULL,
    author_id VARCHAR(64) NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    FOREIGN KEY (batch_id) REFERENCES event_batches(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_message_logs_created_at ON message_logs(created_at);
CREATE INDEX idx_message_logs_author_id ON message_logs(author_id);

CREATE TABLE punishments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    guild_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    moderator_id VARCHAR(64),
    type VARCHAR(64) NOT NULL,
    reason TEXT,
    source_id VARCHAR(128),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_punishments_user_id ON punishments(user_id);
CREATE INDEX idx_punishments_created_at ON punishments(created_at);

CREATE TABLE config_versions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    scope VARCHAR(128) NOT NULL,
    `key` VARCHAR(255) NOT NULL,
    value_json TEXT NOT NULL,
    created_by_discord_id VARCHAR(64),
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (created_by_discord_id) REFERENCES users(discord_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_config_versions_scope_key ON config_versions(scope, `key`);

CREATE TABLE config_acknowledgements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_version_id BIGINT NOT NULL,
    bot_client_name VARCHAR(128) NOT NULL,
    acked_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    UNIQUE (config_version_id, bot_client_name),
    FOREIGN KEY (config_version_id) REFERENCES config_versions(id) ON DELETE CASCADE,
    FOREIGN KEY (bot_client_name) REFERENCES bot_clients(name) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE audit_actions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    actor_discord_id VARCHAR(64),
    action VARCHAR(128) NOT NULL,
    target_type VARCHAR(128) NOT NULL,
    target_id VARCHAR(128),
    metadata_json TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (actor_discord_id) REFERENCES users(discord_id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_audit_actions_created_at ON audit_actions(created_at);

-- +goose Down
DROP TABLE audit_actions;
DROP TABLE config_acknowledgements;
DROP TABLE config_versions;
DROP TABLE punishments;
DROP TABLE message_logs;
DROP TABLE event_batches;
DROP TABLE bot_clients;
DROP TABLE users;
