-- +goose Up
ALTER TABLE event_batches ADD COLUMN metadata_json TEXT NULL;
UPDATE event_batches SET metadata_json = '{}';
ALTER TABLE event_batches MODIFY metadata_json TEXT NOT NULL;

CREATE TABLE registrations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    batch_id VARCHAR(128) NOT NULL,
    guild_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    username VARCHAR(255) NOT NULL,
    registered_at DATETIME(6) NOT NULL,
    metadata_json TEXT NOT NULL,
    FOREIGN KEY (batch_id) REFERENCES event_batches(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_registrations_registered_at ON registrations(registered_at);
CREATE INDEX idx_registrations_user_id ON registrations(user_id);

CREATE TABLE sync_heartbeats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    client_name VARCHAR(128) NOT NULL,
    status VARCHAR(64) NOT NULL,
    details_json TEXT NOT NULL,
    checked_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    FOREIGN KEY (client_name) REFERENCES bot_clients(name) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_sync_heartbeats_client_checked ON sync_heartbeats(client_name, checked_at);

-- +goose Down
DROP TABLE sync_heartbeats;
DROP TABLE registrations;
ALTER TABLE event_batches DROP COLUMN metadata_json;
