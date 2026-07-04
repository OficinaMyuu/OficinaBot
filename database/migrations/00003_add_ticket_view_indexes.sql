-- +goose Up
CREATE INDEX idx_messages_versions_channel_author ON messages_versions(channel_id, author_id);
CREATE INDEX idx_support_tickets_created ON support_tickets(created_at);
CREATE INDEX idx_support_tickets_initiator_created ON support_tickets(initiator_id, created_at);

-- +goose Down
DROP INDEX idx_support_tickets_initiator_created ON support_tickets;
DROP INDEX idx_support_tickets_created ON support_tickets;
DROP INDEX idx_messages_versions_channel_author ON messages_versions;
