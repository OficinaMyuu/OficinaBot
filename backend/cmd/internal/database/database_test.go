package database

import (
	"path/filepath"
	"testing"
)

func TestOpenConfiguresSQLite(t *testing.T) {
	db := openTestDatabase(t)
	defer db.Close()

	assertPragma(t, db, "foreign_keys", "1")
	assertPragma(t, db, "busy_timeout", "5000")
	assertPragma(t, db, "journal_mode", "wal")
}

func TestMigrateCreatesPersistenceTables(t *testing.T) {
	db := openTestDatabase(t)
	defer db.Close()

	if err := db.Migrate(); err != nil {
		t.Fatalf("migrate database: %v", err)
	}

	expectedTables := []string{
		"users",
		"bot_clients",
		"event_batches",
		"message_logs",
		"punishments",
		"config_versions",
		"config_acknowledgements",
		"audit_actions",
		"admin_sessions",
		"registrations",
		"sync_heartbeats",
	}
	for _, table := range expectedTables {
		t.Run(table, func(t *testing.T) {
			assertTableExists(t, db, table)
		})
	}
}

func openTestDatabase(t *testing.T) *Database {
	t.Helper()

	db, err := Open(Config{Path: filepath.Join(t.TempDir(), "test.db")})
	if err != nil {
		t.Fatalf("open database: %v", err)
	}
	return db
}

func assertTableExists(t *testing.T, db *Database, table string) {
	t.Helper()

	var name string
	err := db.SQL.QueryRow(
		"SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
		table,
	).Scan(&name)
	if err != nil {
		t.Fatalf("expected table %s to exist: %v", table, err)
	}
}

func assertPragma(t *testing.T, db *Database, name, want string) {
	t.Helper()

	var got string
	if err := db.SQL.QueryRow("PRAGMA " + name).Scan(&got); err != nil {
		t.Fatalf("read pragma %s: %v", name, err)
	}
	if got != want {
		t.Fatalf("expected pragma %s=%s, got %s", name, want, got)
	}
}
