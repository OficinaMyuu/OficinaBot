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

func openTestDatabase(t *testing.T) *Database {
	t.Helper()

	db, err := Open(Config{Path: filepath.Join(t.TempDir(), "test.db")})
	if err != nil {
		t.Fatalf("open database: %v", err)
	}
	return db
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
