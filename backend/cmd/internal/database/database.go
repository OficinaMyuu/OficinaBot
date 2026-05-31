package database

import (
	"database/sql"
	"fmt"
	"os"
	"path/filepath"

	"github.com/glebarez/sqlite"
	"github.com/pressly/goose/v3"
	"gorm.io/gorm"
)

type Config struct {
	Path string
}

type Database struct {
	Gorm *gorm.DB
	SQL  *sql.DB
}

func Open(cfg Config) (*Database, error) {
	if cfg.Path == "" {
		return nil, fmt.Errorf("database path is required")
	}

	if err := os.MkdirAll(filepath.Dir(cfg.Path), 0755); err != nil {
		return nil, fmt.Errorf("create database directory: %w", err)
	}

	gormDB, err := gorm.Open(sqlite.Open(cfg.Path), &gorm.Config{})
	if err != nil {
		return nil, fmt.Errorf("open sqlite database: %w", err)
	}

	sqlDB, err := gormDB.DB()
	if err != nil {
		return nil, fmt.Errorf("get sql database: %w", err)
	}

	if err := configureSQLite(sqlDB); err != nil {
		sqlDB.Close()
		return nil, err
	}

	return &Database{Gorm: gormDB, SQL: sqlDB}, nil
}

func (db *Database) Close() error {
	return db.SQL.Close()
}

func (db *Database) Migrate() error {
	goose.SetBaseFS(migrationsFS)
	if err := goose.SetDialect("sqlite3"); err != nil {
		return fmt.Errorf("set goose dialect: %w", err)
	}
	if err := goose.Up(db.SQL, "migrations"); err != nil {
		return fmt.Errorf("run migrations: %w", err)
	}
	return nil
}

func configureSQLite(db *sql.DB) error {
	if _, err := db.Exec("PRAGMA journal_mode = WAL"); err != nil {
		return fmt.Errorf("enable wal mode: %w", err)
	}
	if _, err := db.Exec("PRAGMA foreign_keys = ON"); err != nil {
		return fmt.Errorf("enable foreign keys: %w", err)
	}
	if _, err := db.Exec("PRAGMA busy_timeout = 5000"); err != nil {
		return fmt.Errorf("set busy timeout: %w", err)
	}
	if _, err := db.Exec("PRAGMA synchronous = NORMAL"); err != nil {
		return fmt.Errorf("set synchronous mode: %w", err)
	}
	return nil
}
