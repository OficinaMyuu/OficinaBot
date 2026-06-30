package database_test

import (
	"testing"

	"oficina-img/internal/database"
	"oficina-img/internal/databasetest"
)

func TestOpenRequiresDSN(t *testing.T) {
	_, err := database.Open(database.Config{})

	if err == nil {
		t.Fatal("expected missing database DSN error")
	}
}

func TestMigrateCreatesPersistenceTables(t *testing.T) {
	db := databasetest.OpenMigrated(t)
	defer databasetest.Close(t, db)

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
			databasetest.RequireTableExists(t, db.SQL, table)
		})
	}
}
