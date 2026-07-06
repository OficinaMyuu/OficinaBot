package store

import (
	"context"
	"database/sql"
	"os"
	"testing"
	"time"

	_ "github.com/go-sql-driver/mysql"

	"oficina-registrar/internal/registration"
)

func TestRegisterRepositorySaveLiveMySQL(t *testing.T) {
	dsn := os.Getenv("OFICINA_TEST_MYSQL_DSN")
	if dsn == "" {
		t.Skip("set OFICINA_TEST_MYSQL_DSN to run live MySQL repository coverage")
	}

	db, err := sql.Open("mysql", dsn)
	if err != nil {
		t.Fatalf("sql.Open() error = %v", err)
	}
	defer db.Close()
	db.SetMaxOpenConns(1)
	db.SetMaxIdleConns(1)

	ctx := context.Background()
	if _, err := db.ExecContext(ctx, `
CREATE TEMPORARY TABLE registers (
	id INT PRIMARY KEY AUTO_INCREMENT,
	target_id BIGINT NOT NULL,
	moderator_id BIGINT NOT NULL,
	age INT NOT NULL,
	gender VARCHAR(64) NOT NULL,
	device VARCHAR(64) NOT NULL,
	created_at BIGINT NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
`); err != nil {
		t.Fatalf("create temporary registers table: %v", err)
	}

	now := time.Date(2026, 7, 5, 12, 30, 0, 0, time.UTC)
	repository := NewRegisterRepository(db, func() time.Time { return now })
	if err := repository.Save(ctx, RegisterRecord{
		TargetID:    123,
		ModeratorID: 456,
		Age:         12,
		Gender:      registration.GenderNonBinary,
		Device:      registration.DeviceMobile,
	}); err != nil {
		t.Fatalf("Save() error = %v", err)
	}

	var count int
	if err := db.QueryRowContext(ctx, `
SELECT COUNT(*)
FROM registers
WHERE target_id = ? AND moderator_id = ? AND age = ? AND gender = ? AND device = ? AND created_at = ?
`, 123, 456, 12, "NON_BINARY", "MOBILE", now.UnixMilli()).Scan(&count); err != nil {
		t.Fatalf("select inserted register: %v", err)
	}
	if count != 1 {
		t.Fatalf("inserted rows = %d, want 1", count)
	}
}
