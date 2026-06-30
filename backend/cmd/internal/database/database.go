package database

import (
	"database/sql"
	"fmt"
	"time"

	"github.com/pressly/goose/v3"
	"gorm.io/driver/mysql"
	"gorm.io/gorm"
)

type Config struct {
	DSN string
}

type Database struct {
	Gorm *gorm.DB
	SQL  *sql.DB
}

func Open(cfg Config) (*Database, error) {
	if cfg.DSN == "" {
		return nil, fmt.Errorf("database DSN is required")
	}

	gormDB, err := gorm.Open(mysql.Open(cfg.DSN), &gorm.Config{})
	if err != nil {
		return nil, fmt.Errorf("open mysql database: %w", err)
	}

	sqlDB, err := gormDB.DB()
	if err != nil {
		return nil, fmt.Errorf("get sql database: %w", err)
	}

	configureConnectionPool(sqlDB)

	return &Database{Gorm: gormDB, SQL: sqlDB}, nil
}

func (db *Database) Close() error {
	return db.SQL.Close()
}

func (db *Database) Migrate() error {
	goose.SetBaseFS(migrationsFS)
	if err := goose.SetDialect("mysql"); err != nil {
		return fmt.Errorf("set goose dialect: %w", err)
	}
	if err := goose.Up(db.SQL, "migrations"); err != nil {
		return fmt.Errorf("run migrations: %w", err)
	}
	return nil
}

func configureConnectionPool(db *sql.DB) {
	db.SetConnMaxLifetime(5 * time.Minute)
	db.SetMaxOpenConns(10)
	db.SetMaxIdleConns(5)
}
