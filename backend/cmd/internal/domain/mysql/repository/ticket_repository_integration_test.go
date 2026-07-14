package repository

import (
	"context"
	"database/sql"
	"os"
	"testing"
)

func TestTicketRepositoryIntegrationListAndMessages(t *testing.T) {
	dsn := testMySQLDSN(t)
	db := openTemporaryMySQLSchema(t, dsn)
	createTicketTables(t, db)
	repository := NewTicketRepository(db)
	messageRepository := NewMessageRepository(db)
	ctx := context.Background()

	insertTicketFixtures(t, db)

	tickets, err := repository.List(ctx, TicketListFilter{Status: "open", Limit: 1})
	if err != nil {
		t.Fatalf("list tickets: %v", err)
	}
	if len(tickets.Tickets) != 1 || tickets.Tickets[0].ID != 2 {
		t.Fatalf("expected newest open ticket, got %+v", tickets)
	}
	if tickets.NextCursor == nil || tickets.NextCursor.ID != 2 {
		t.Fatalf("expected next cursor for over-limit page, got %+v", tickets.NextCursor)
	}

	ticket, err := repository.Find(ctx, 1)
	if err != nil {
		t.Fatalf("find ticket: %v", err)
	}
	if ticket.ClosedByID == nil || ticket.Status() != "closed" {
		t.Fatalf("expected closed ticket metadata, got %+v", ticket)
	}

	messages, err := messageRepository.List(ctx, 456, MessageFilter{Limit: 10})
	if err != nil {
		t.Fatalf("list messages: %v", err)
	}
	if len(messages.Messages) != 5 {
		t.Fatalf("expected five folded messages, got %+v", messages.Messages)
	}
	first := messages.Messages[0]
	if first.MessageID != 100 || !first.IsEdited || first.Content == nil || *first.Content != "edited hello" {
		t.Fatalf("expected edited first message, got %+v", first)
	}
	second := messages.Messages[1]
	if second.MessageID != 101 || !second.IsDeleted || second.DeletedByID == nil || second.Content == nil || *second.Content != "remove me" {
		t.Fatalf("expected deleted second message with original content, got %+v", second)
	}

	latest, err := messageRepository.List(ctx, 456, MessageFilter{Limit: 2})
	if err != nil {
		t.Fatalf("list latest messages: %v", err)
	}
	if latest.Messages[0].MessageID != 103 || latest.Messages[1].MessageID != 104 || !latest.HasMoreBefore || latest.HasMoreAfter {
		t.Fatalf("expected chronological latest page, got %+v", latest)
	}
	before := int64(103)
	older, err := messageRepository.List(ctx, 456, MessageFilter{Limit: 2, BeforeID: &before})
	if err != nil {
		t.Fatalf("list older messages: %v", err)
	}
	if older.Messages[0].MessageID != 101 || older.Messages[1].MessageID != 102 || !older.HasMoreBefore || !older.HasMoreAfter {
		t.Fatalf("expected page before message 103, got %+v", older)
	}
	after := int64(102)
	newer, err := messageRepository.List(ctx, 456, MessageFilter{Limit: 2, AfterID: &after})
	if err != nil {
		t.Fatalf("list newer messages: %v", err)
	}
	if newer.Messages[0].MessageID != 103 || newer.Messages[1].MessageID != 104 || !newer.HasMoreBefore || newer.HasMoreAfter {
		t.Fatalf("expected page after message 102, got %+v", newer)
	}
	around := int64(102)
	centered, err := messageRepository.List(ctx, 456, MessageFilter{Limit: 3, AroundID: &around})
	if err != nil {
		t.Fatalf("list messages around anchor: %v", err)
	}
	if centered.Messages[0].MessageID != 101 || centered.Messages[1].MessageID != 102 || centered.Messages[2].MessageID != 103 {
		t.Fatalf("expected centered page around message 102, got %+v", centered)
	}
}

func testMySQLDSN(t *testing.T) string {
	t.Helper()
	dsn := os.Getenv("OFICINA_TEST_MYSQL_DSN")
	if dsn == "" {
		t.Skip("set OFICINA_TEST_MYSQL_DSN to run live MySQL ticket repository coverage")
	}
	return dsn
}

func createTicketTables(t *testing.T, db *sql.DB) {
	t.Helper()
	statements := []string{
		`CREATE TABLE users (
			id BIGINT PRIMARY KEY,
			name VARCHAR(255) NOT NULL,
			global_name VARCHAR(255),
			avatar_hash VARCHAR(128),
			is_bot BOOLEAN NOT NULL DEFAULT FALSE,
			created_at BIGINT NOT NULL,
			updated_at BIGINT NOT NULL
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`,
		`CREATE TABLE support_tickets (
			id INT PRIMARY KEY AUTO_INCREMENT,
			title VARCHAR(255) NOT NULL,
			description TEXT NOT NULL,
			guild_id BIGINT NOT NULL,
			channel_id BIGINT NOT NULL,
			initiator_id BIGINT NOT NULL,
			close_reason TEXT,
			closed_by_id BIGINT,
			merged_into INT,
			created_at BIGINT NOT NULL,
			updated_at BIGINT NOT NULL,
			UNIQUE (channel_id)
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`,
		`CREATE TABLE messages_versions (
			id INT PRIMARY KEY AUTO_INCREMENT,
			message_id BIGINT NOT NULL,
			author_id BIGINT NOT NULL,
			channel_id BIGINT NOT NULL,
			message_ref_id BIGINT,
			content TEXT,
			sticker_id BIGINT,
			is_deleted BOOLEAN NOT NULL,
			is_original BOOLEAN NOT NULL DEFAULT FALSE,
			deleted_by_id BIGINT,
			created_at BIGINT NOT NULL
		) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`,
	}
	for _, statement := range statements {
		if _, err := db.Exec(statement); err != nil {
			t.Fatalf("create ticket fixture table: %v", err)
		}
	}
}

func insertTicketFixtures(t *testing.T, db *sql.DB) {
	t.Helper()
	statements := []string{
		`INSERT INTO users (id, name, global_name, created_at, updated_at) VALUES
			(42, 'myuu', 'Myuu', 1, 1),
			(99, 'staff', 'Staff', 1, 1),
			(1000, 'helper', NULL, 1, 1)`,
		`INSERT INTO support_tickets (id, title, description, guild_id, channel_id, initiator_id, close_reason, closed_by_id, merged_into, created_at, updated_at) VALUES
			(1, 'Closed ticket', 'done', 123, 456, 42, 'Solved', 99, NULL, 10, 20),
			(2, 'Open ticket', 'still running', 123, 789, 42, NULL, NULL, NULL, 30, 30),
			(3, 'Older open ticket', 'still running too', 123, 790, 42, NULL, NULL, NULL, 25, 25)`,
		`INSERT INTO messages_versions (message_id, author_id, channel_id, message_ref_id, content, sticker_id, is_deleted, is_original, deleted_by_id, created_at) VALUES
			(100, 42, 456, NULL, 'hello', NULL, false, true, NULL, 1000),
			(100, 42, 456, NULL, 'edited hello', NULL, false, false, NULL, 1010),
			(101, 1000, 456, 100, 'remove me', NULL, false, true, NULL, 1020),
			(101, 0, 456, NULL, NULL, NULL, true, false, 99, 1030),
			(102, 42, 456, NULL, 'third', NULL, false, true, NULL, 1040),
			(103, 42, 456, NULL, 'fourth', NULL, false, true, NULL, 1050),
			(104, 42, 456, NULL, 'fifth', NULL, false, true, NULL, 1060)`,
	}
	for _, statement := range statements {
		if _, err := db.Exec(statement); err != nil {
			t.Fatalf("insert ticket fixture: %v", err)
		}
	}
}
