package repository

import (
	"context"
	"errors"
	"testing"

	"oficina-img/internal/domain/entity"
)

func TestDashboardSessionRepositoryIntegrationLifecycle(t *testing.T) {
	db := openTemporaryMySQLSchema(t, testMySQLDSN(t))
	if _, err := db.Exec(`CREATE TABLE dashboard_sessions (
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
	) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`); err != nil {
		t.Fatalf("create dashboard sessions table: %v", err)
	}

	repository := NewDashboardSessionRepository(openTestGORM(t, db))
	ctx := context.Background()
	session := entity.DashboardSession{
		User: entity.DashboardUser{
			ID: "42", Username: "myuu", GuildName: "Oficina",
			Permissions: "32",
		},
		CSRFToken: "csrf-one",
		ExpiresAt: 200,
	}
	if err := repository.Save(ctx, "session-one", session); err != nil {
		t.Fatalf("save session: %v", err)
	}

	found, err := repository.Find(ctx, "session-one")
	if err != nil {
		t.Fatalf("find session: %v", err)
	}
	if found.User.ID != "42" || found.User.GlobalName != nil || found.CSRFToken != "csrf-one" {
		t.Fatalf("unexpected initial session: %+v", found)
	}

	if _, err := db.Exec("UPDATE dashboard_sessions SET created_at = 123, updated_at = 123 WHERE session_id_hash = 'session-one'"); err != nil {
		t.Fatalf("prepare timestamp assertion: %v", err)
	}
	globalName := "Oficina Myuu"
	avatarURL := "https://cdn.example/avatar.png"
	session.User.GlobalName = &globalName
	session.User.AvatarURL = &avatarURL
	session.CSRFToken = "csrf-two"
	session.ExpiresAt = 300
	if err := repository.Save(ctx, "session-one", session); err != nil {
		t.Fatalf("upsert session: %v", err)
	}

	found, err = repository.Find(ctx, "session-one")
	if err != nil {
		t.Fatalf("find updated session: %v", err)
	}
	if found.User.GlobalName == nil || *found.User.GlobalName != globalName || found.CSRFToken != "csrf-two" || found.ExpiresAt != 300 {
		t.Fatalf("unexpected updated session: %+v", found)
	}
	var createdAt, updatedAt int64
	if err := db.QueryRow("SELECT created_at, updated_at FROM dashboard_sessions WHERE session_id_hash = 'session-one'").Scan(&createdAt, &updatedAt); err != nil {
		t.Fatalf("read session timestamps: %v", err)
	}
	if createdAt != 123 || updatedAt == 123 {
		t.Fatalf("expected upsert to preserve created_at and refresh updated_at, got %d/%d", createdAt, updatedAt)
	}

	expired := session
	expired.ExpiresAt = 100
	if err := repository.Save(ctx, "session-expired", expired); err != nil {
		t.Fatalf("save expired session: %v", err)
	}
	if err := repository.DeleteExpired(ctx, 150); err != nil {
		t.Fatalf("delete expired sessions: %v", err)
	}
	if _, err := repository.Find(ctx, "session-expired"); !errors.Is(err, ErrSessionNotFound) {
		t.Fatalf("expected expired session to be deleted, got %v", err)
	}
	if _, err := repository.Find(ctx, "session-one"); err != nil {
		t.Fatalf("expected active session to remain: %v", err)
	}

	if err := repository.Delete(ctx, "session-one"); err != nil {
		t.Fatalf("delete session: %v", err)
	}
	if _, err := repository.Find(ctx, "session-one"); !errors.Is(err, ErrSessionNotFound) {
		t.Fatalf("expected deleted session to be missing, got %v", err)
	}
}
