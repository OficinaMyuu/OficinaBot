-- +goose Up
CREATE TABLE users (
    discord_id TEXT PRIMARY KEY,
    username TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE bot_clients (
    name TEXT PRIMARY KEY,
    token_hash TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at DATETIME
);

CREATE TABLE event_batches (
    id TEXT PRIMARY KEY,
    client_name TEXT NOT NULL,
    kind TEXT NOT NULL,
    received_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (client_name) REFERENCES bot_clients(name) ON DELETE RESTRICT
);

CREATE TABLE message_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    batch_id TEXT NOT NULL,
    guild_id TEXT NOT NULL,
    channel_id TEXT NOT NULL,
    message_id TEXT NOT NULL,
    author_id TEXT NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL,
    FOREIGN KEY (batch_id) REFERENCES event_batches(id) ON DELETE CASCADE
);

CREATE INDEX idx_message_logs_created_at ON message_logs(created_at);
CREATE INDEX idx_message_logs_author_id ON message_logs(author_id);

CREATE TABLE punishments (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    guild_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    moderator_id TEXT,
    type TEXT NOT NULL,
    reason TEXT,
    source_id TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_punishments_user_id ON punishments(user_id);
CREATE INDEX idx_punishments_created_at ON punishments(created_at);

CREATE TABLE config_versions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    scope TEXT NOT NULL,
    key TEXT NOT NULL,
    value_json TEXT NOT NULL,
    created_by_discord_id TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (created_by_discord_id) REFERENCES users(discord_id) ON DELETE SET NULL
);

CREATE INDEX idx_config_versions_scope_key ON config_versions(scope, key);

CREATE TABLE config_acknowledgements (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    config_version_id INTEGER NOT NULL,
    bot_client_name TEXT NOT NULL,
    acked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (config_version_id, bot_client_name),
    FOREIGN KEY (config_version_id) REFERENCES config_versions(id) ON DELETE CASCADE,
    FOREIGN KEY (bot_client_name) REFERENCES bot_clients(name) ON DELETE CASCADE
);

CREATE TABLE audit_actions (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    actor_discord_id TEXT,
    action TEXT NOT NULL,
    target_type TEXT NOT NULL,
    target_id TEXT,
    metadata_json TEXT NOT NULL DEFAULT '{}',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (actor_discord_id) REFERENCES users(discord_id) ON DELETE SET NULL
);

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
