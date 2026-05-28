package repository

import (
	"context"
	"path/filepath"
	"testing"
	"time"

	"oficina-img/internal/database"
)

func TestUserRepositoryCreatesAndListsUsers(t *testing.T) {
	db := openMigratedDatabase(t)
	ctx := context.Background()
	repo := NewUserRepository(db.Gorm)

	if err := repo.Create(ctx, &User{DiscordID: "100", Username: "Leonardo"}); err != nil {
		t.Fatalf("create user: %v", err)
	}

	user, err := repo.Get(ctx, "100")
	if err != nil {
		t.Fatalf("get user: %v", err)
	}
	if user.Username != "Leonardo" {
		t.Fatalf("expected username Leonardo, got %q", user.Username)
	}

	users, err := repo.List(ctx)
	if err != nil {
		t.Fatalf("list users: %v", err)
	}
	if len(users) != 1 {
		t.Fatalf("expected 1 user, got %d", len(users))
	}
}

func TestBotClientRepositoryTouchesLastSeenAt(t *testing.T) {
	db := openMigratedDatabase(t)
	ctx := context.Background()
	repo := NewBotClientRepository(db.Gorm)

	if err := repo.Create(ctx, &BotClient{Name: "bot", TokenHash: "hash"}); err != nil {
		t.Fatalf("create bot client: %v", err)
	}
	if err := repo.Touch(ctx, "bot", nowForTest); err != nil {
		t.Fatalf("touch bot client: %v", err)
	}

	client, err := repo.Get(ctx, "bot")
	if err != nil {
		t.Fatalf("get bot client: %v", err)
	}
	if client.LastSeenAt == nil || !client.LastSeenAt.Equal(nowForTest) {
		t.Fatalf("expected last seen at %v, got %v", nowForTest, client.LastSeenAt)
	}
}

func TestEventAndMessageRepositoriesPersistBatchLogs(t *testing.T) {
	db := openMigratedDatabase(t)
	ctx := context.Background()

	if err := NewBotClientRepository(db.Gorm).Create(ctx, &BotClient{Name: "bot", TokenHash: "hash"}); err != nil {
		t.Fatalf("create bot client: %v", err)
	}
	if err := NewEventBatchRepository(db.Gorm).Create(ctx, &EventBatch{ID: "batch-1", ClientName: "bot", Kind: "message_logs"}); err != nil {
		t.Fatalf("create event batch: %v", err)
	}

	logs := []MessageLog{{
		BatchID:   "batch-1",
		GuildID:   "guild",
		ChannelID: "channel",
		MessageID: "message",
		AuthorID:  "author",
		Content:   "hello",
	}}
	if err := NewMessageLogRepository(db.Gorm).CreateMany(ctx, logs); err != nil {
		t.Fatalf("create message logs: %v", err)
	}

	var count int64
	if err := db.Gorm.Table("message_logs").Count(&count).Error; err != nil {
		t.Fatalf("count message logs: %v", err)
	}
	if count != 1 {
		t.Fatalf("expected 1 message log, got %d", count)
	}
}

func TestPunishmentRepositoryListsRecent(t *testing.T) {
	db := openMigratedDatabase(t)
	ctx := context.Background()
	repo := NewPunishmentRepository(db.Gorm)

	if err := repo.Create(ctx, &Punishment{GuildID: "guild", UserID: "user", Type: "WARN"}); err != nil {
		t.Fatalf("create punishment: %v", err)
	}

	punishments, err := repo.ListRecent(ctx, 10)
	if err != nil {
		t.Fatalf("list recent punishments: %v", err)
	}
	if len(punishments) != 1 || punishments[0].Type != "WARN" {
		t.Fatalf("expected WARN punishment, got %+v", punishments)
	}
}

func TestConfigVersionRepositoryTracksPendingAcknowledgements(t *testing.T) {
	db := openMigratedDatabase(t)
	ctx := context.Background()

	if err := NewUserRepository(db.Gorm).Create(ctx, &User{DiscordID: "100", Username: "Leonardo"}); err != nil {
		t.Fatalf("create user: %v", err)
	}
	if err := NewBotClientRepository(db.Gorm).Create(ctx, &BotClient{Name: "bot", TokenHash: "hash"}); err != nil {
		t.Fatalf("create bot client: %v", err)
	}

	actorID := "100"
	repo := NewConfigVersionRepository(db.Gorm)
	version := &ConfigVersion{
		Scope:              "automod",
		Key:                "bad_words",
		ValueJSON:          `["bad"]`,
		CreatedByDiscordID: &actorID,
	}
	if err := repo.Create(ctx, version); err != nil {
		t.Fatalf("create config version: %v", err)
	}

	pending, err := repo.PendingForClient(ctx, "bot")
	if err != nil {
		t.Fatalf("list pending configs: %v", err)
	}
	if len(pending) != 1 {
		t.Fatalf("expected 1 pending config, got %d", len(pending))
	}

	if err := repo.Acknowledge(ctx, version.ID, "bot"); err != nil {
		t.Fatalf("ack config version: %v", err)
	}
	pending, err = repo.PendingForClient(ctx, "bot")
	if err != nil {
		t.Fatalf("list pending configs after ack: %v", err)
	}
	if len(pending) != 0 {
		t.Fatalf("expected no pending configs after ack, got %d", len(pending))
	}
}

func TestAuditActionRepositoryListsRecent(t *testing.T) {
	db := openMigratedDatabase(t)
	ctx := context.Background()

	if err := NewUserRepository(db.Gorm).Create(ctx, &User{DiscordID: "100", Username: "Leonardo"}); err != nil {
		t.Fatalf("create user: %v", err)
	}

	actorID := "100"
	repo := NewAuditActionRepository(db.Gorm)
	if err := repo.Create(ctx, &AuditAction{
		ActorDiscordID: &actorID,
		Action:         "config.update",
		TargetType:     "config",
		MetadataJSON:   `{"key":"bad_words"}`,
	}); err != nil {
		t.Fatalf("create audit action: %v", err)
	}

	actions, err := repo.ListRecent(ctx, 10)
	if err != nil {
		t.Fatalf("list recent audit actions: %v", err)
	}
	if len(actions) != 1 || actions[0].Action != "config.update" {
		t.Fatalf("expected config.update audit action, got %+v", actions)
	}
}

var nowForTest = mustParseTime("2026-05-27T12:00:00Z")

func mustParseTime(value string) time.Time {
	parsed, err := time.Parse(time.RFC3339, value)
	if err != nil {
		panic(err)
	}
	return parsed
}

func openMigratedDatabase(t *testing.T) *database.Database {
	t.Helper()

	db, err := database.Open(database.Config{Path: filepath.Join(t.TempDir(), "test.db")})
	if err != nil {
		t.Fatalf("open database: %v", err)
	}
	t.Cleanup(func() {
		if err := db.Close(); err != nil {
			t.Fatalf("close database: %v", err)
		}
	})

	if err := db.Migrate(); err != nil {
		t.Fatalf("migrate database: %v", err)
	}
	return db
}
