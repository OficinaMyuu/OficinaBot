package repository

import (
	"context"
	"database/sql"
	"fmt"
	"os"
	"strings"
	"testing"
	"time"

	"github.com/go-sql-driver/mysql"
	_ "github.com/go-sql-driver/mysql"
	"oficina-img/internal/domain/entity"
)

func TestBirthdayRepositoryIntegrationCRUD(t *testing.T) {
	dsn := os.Getenv("OFICINA_TEST_MYSQL_DSN")
	if dsn == "" {
		t.Skip("set OFICINA_TEST_MYSQL_DSN to run live MySQL birthday repository coverage")
	}

	db := openTemporaryMySQLSchema(t, dsn)
	repository := NewBirthdayRepository(db)
	ctx := context.Background()

	created, err := repository.Create(ctx, entity.Birthday{
		UserID:    42,
		Name:      "Myuu",
		Birthday:  mustDate(t, "2020-05-10"),
		ZoneHours: -3,
	})
	if err != nil {
		t.Fatalf("create birthday: %v", err)
	}
	if created.CreatedAt == 0 || created.UpdatedAt == 0 {
		t.Fatalf("expected timestamps, got %+v", created)
	}

	if _, err := repository.Create(ctx, created); err != ErrDuplicateBirthday {
		t.Fatalf("expected duplicate error, got %v", err)
	}

	list, err := repository.List(ctx, BirthdayFilter{Search: "my", Month: 5})
	if err != nil {
		t.Fatalf("list birthdays: %v", err)
	}
	if len(list) != 1 || list[0].UserID != 42 {
		t.Fatalf("expected created birthday in list, got %+v", list)
	}

	updated, err := repository.Update(ctx, entity.Birthday{
		UserID:    42,
		Name:      "Oficina Myuu",
		Birthday:  mustDate(t, "2020-06-11"),
		ZoneHours: -2,
	})
	if err != nil {
		t.Fatalf("update birthday: %v", err)
	}
	if updated.Name != "Oficina Myuu" || updated.Birthday.Format(DateOnlyLayout) != "2020-06-11" {
		t.Fatalf("unexpected update result: %+v", updated)
	}

	if err := repository.Delete(ctx, 42); err != nil {
		t.Fatalf("delete birthday: %v", err)
	}
	if err := repository.Delete(ctx, 42); err != ErrBirthdayNotFound {
		t.Fatalf("expected not found after delete, got %v", err)
	}
}

func openTemporaryMySQLSchema(t *testing.T, dsn string) *sql.DB {
	t.Helper()

	cfg, err := mysql.ParseDSN(dsn)
	if err != nil {
		t.Fatalf("parse mysql dsn: %v", err)
	}

	schemaName := fmt.Sprintf("oficina_backend_test_%d", time.Now().UnixNano())
	serverCfg := *cfg
	serverCfg.DBName = ""
	adminDB, err := sql.Open("mysql", serverCfg.FormatDSN())
	if err != nil {
		t.Fatalf("open mysql server connection: %v", err)
	}
	t.Cleanup(func() {
		adminDB.Close()
	})

	if _, err := adminDB.Exec("CREATE DATABASE " + schemaName + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"); err != nil {
		t.Fatalf("create temporary schema: %v", err)
	}
	t.Cleanup(func() {
		_, _ = adminDB.Exec("DROP DATABASE " + schemaName)
	})

	schemaCfg := *cfg
	schemaCfg.DBName = schemaName
	schemaCfg.ParseTime = true
	db, err := sql.Open("mysql", schemaCfg.FormatDSN())
	if err != nil {
		t.Fatalf("open mysql schema connection: %v", err)
	}
	t.Cleanup(func() {
		db.Close()
	})

	ddl := strings.Join([]string{
		"CREATE TABLE birthdays (",
		"user_id BIGINT PRIMARY KEY,",
		"name VARCHAR(255) NOT NULL,",
		"birthday DATE NOT NULL,",
		"zone_hours INT NOT NULL DEFAULT -3,",
		"created_at BIGINT NOT NULL,",
		"updated_at BIGINT NOT NULL",
		") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
	}, " ")
	if _, err := db.Exec(ddl); err != nil {
		t.Fatalf("create birthdays table: %v", err)
	}

	return db
}

func mustDate(t *testing.T, value string) time.Time {
	t.Helper()
	parsed, err := ParseBirthdayDate(value)
	if err != nil {
		t.Fatalf("parse date %q: %v", value, err)
	}
	return parsed
}
