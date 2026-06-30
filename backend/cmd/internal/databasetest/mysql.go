package databasetest

import (
	"database/sql"
	"os"
	"strconv"
	"time"

	mysqldriver "github.com/go-sql-driver/mysql"
	"oficina-img/internal/database"
)

const EnvMySQLDSN = "OFICINA_TEST_MYSQL_DSN"

type TB interface {
	Helper()
	Cleanup(func())
	Fatalf(format string, args ...any)
	Skipf(format string, args ...any)
}

func OpenMigrated(t TB) *database.Database {
	t.Helper()

	db := Open(t)
	if err := db.Migrate(); err != nil {
		t.Fatalf("migrate mysql database: %v", err)
	}
	return db
}

func Open(t TB) *database.Database {
	t.Helper()

	cfg := Config(t)
	db, err := database.Open(cfg)
	if err != nil {
		t.Fatalf("open mysql database: %v", err)
	}
	return db
}

func Config(t TB) database.Config {
	t.Helper()

	rawDSN := os.Getenv(EnvMySQLDSN)
	if rawDSN == "" {
		t.Skipf("%s is not set; skipping live MySQL integration test", EnvMySQLDSN)
	}

	parsed, err := mysqldriver.ParseDSN(rawDSN)
	if err != nil {
		t.Fatalf("parse %s: %v", EnvMySQLDSN, err)
	}

	schema := "oficina_test_" + strconv.FormatInt(time.Now().UnixNano(), 36)
	serverConfig := parsed.Clone()
	serverConfig.DBName = ""

	adminDB, err := sql.Open("mysql", serverConfig.FormatDSN())
	if err != nil {
		t.Fatalf("open mysql admin connection: %v", err)
	}

	if _, err := adminDB.Exec("CREATE DATABASE " + quoteIdentifier(schema) + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"); err != nil {
		_ = adminDB.Close()
		t.Fatalf("create mysql test database: %v", err)
	}

	t.Cleanup(func() {
		_, _ = adminDB.Exec("DROP DATABASE IF EXISTS " + quoteIdentifier(schema))
		_ = adminDB.Close()
	})

	parsed.DBName = schema
	parsed.ParseTime = true
	if parsed.Params == nil {
		parsed.Params = map[string]string{}
	}
	parsed.Params["charset"] = "utf8mb4"
	parsed.Params["collation"] = "utf8mb4_unicode_ci"

	return database.Config{DSN: parsed.FormatDSN()}
}

func Close(t TB, db *database.Database) {
	t.Helper()

	if err := db.Close(); err != nil {
		t.Fatalf("close mysql database: %v", err)
	}
}

func RequireTableExists(t TB, sqlDB *sql.DB, table string) {
	t.Helper()

	var name string
	err := sqlDB.QueryRow(
		"SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?",
		table,
	).Scan(&name)
	if err != nil {
		t.Fatalf("expected table %s to exist: %v", table, err)
	}
	if name != table {
		t.Fatalf("expected table %s, got %s", table, name)
	}
}

func quoteIdentifier(identifier string) string {
	quoted := "`"
	for _, r := range identifier {
		if r == '`' {
			quoted += "``"
			continue
		}
		quoted += string(r)
	}
	return quoted + "`"
}
