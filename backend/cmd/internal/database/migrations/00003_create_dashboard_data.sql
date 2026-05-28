-- +goose Up
ALTER TABLE event_batches ADD COLUMN metadata_json TEXT NOT NULL DEFAULT '{}';

CREATE TABLE registrations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    batch_id TEXT NOT NULL,
    guild_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    username TEXT NOT NULL,
    registered_at DATETIME NOT NULL,
    metadata_json TEXT NOT NULL DEFAULT '{}',
    FOREIGN KEY (batch_id) REFERENCES event_batches(id) ON DELETE CASCADE
);

CREATE INDEX idx_registrations_registered_at ON registrations(registered_at);
CREATE INDEX idx_registrations_user_id ON registrations(user_id);

CREATE TABLE sync_heartbeats (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    client_name TEXT NOT NULL,
    status TEXT NOT NULL,
    details_json TEXT NOT NULL DEFAULT '{}',
    checked_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (client_name) REFERENCES bot_clients(name) ON DELETE CASCADE
);

CREATE INDEX idx_sync_heartbeats_client_checked ON sync_heartbeats(client_name, checked_at);

-- +goose Down
DROP TABLE sync_heartbeats;
DROP TABLE registrations;
