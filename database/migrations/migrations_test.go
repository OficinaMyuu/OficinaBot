package migrations

import (
	"strings"
	"testing"
)

func TestUsersIsBotMigrationIsReversibleAndDefaultsToFalse(t *testing.T) {
	sql, err := fs.ReadFile("00007_add_users_is_bot.sql")
	if err != nil {
		t.Fatalf("read users is_bot migration: %v", err)
	}

	contents := string(sql)
	for _, expected := range []string{
		"-- +goose Up",
		"ADD COLUMN is_bot BOOLEAN NOT NULL DEFAULT FALSE",
		"-- +goose Down",
		"DROP COLUMN is_bot",
	} {
		if !strings.Contains(contents, expected) {
			t.Fatalf("expected migration to contain %q", expected)
		}
	}
}
