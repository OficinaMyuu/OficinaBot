package migrations

import (
	"database/sql"
	"embed"
	"fmt"

	"github.com/pressly/goose/v3"
)

//go:embed *.sql
var fs embed.FS

const dir = "."

func Up(db *sql.DB) error {
	if err := configureGoose(); err != nil {
		return err
	}
	if err := goose.Up(db, dir); err != nil {
		return fmt.Errorf("run migrations: %w", err)
	}
	return nil
}

func Status(db *sql.DB) error {
	if err := configureGoose(); err != nil {
		return err
	}
	if err := goose.Status(db, dir); err != nil {
		return fmt.Errorf("show migration status: %w", err)
	}
	return nil
}

func Version(db *sql.DB) error {
	if err := configureGoose(); err != nil {
		return err
	}
	if err := goose.Version(db, dir); err != nil {
		return fmt.Errorf("show migration version: %w", err)
	}
	return nil
}

func configureGoose() error {
	goose.SetBaseFS(fs)
	if err := goose.SetDialect("mysql"); err != nil {
		return fmt.Errorf("set goose dialect: %w", err)
	}
	return nil
}
