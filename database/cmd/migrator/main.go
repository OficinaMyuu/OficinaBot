package main

import (
	"database/sql"
	"fmt"
	"net"
	"os"
	"time"

	mysqldriver "github.com/go-sql-driver/mysql"
	_ "github.com/go-sql-driver/mysql"
	"oficina-database/migrations"
)

const (
	defaultDatabasePort      = "3306"
	defaultDatabaseName      = "oficina_services"
	defaultDatabaseCharset   = "utf8mb4"
	defaultDatabaseCollation = "utf8mb4_unicode_ci"
)

func main() {
	if err := run(os.Args[1:]); err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
}

func run(args []string) error {
	command := "up"
	if len(args) > 0 {
		command = args[0]
	}

	dsn, err := databaseDSN()
	if err != nil {
		return err
	}

	db, err := sql.Open("mysql", dsn)
	if err != nil {
		return fmt.Errorf("open mysql connection: %w", err)
	}
	defer db.Close()
	configurePool(db)

	switch command {
	case "up":
		return migrations.Up(db)
	case "status":
		return migrations.Status(db)
	case "version":
		return migrations.Version(db)
	default:
		return fmt.Errorf("unknown command %q; expected up, status, or version", command)
	}
}

func databaseDSN() (string, error) {
	host := os.Getenv("DATABASE_HOST")
	port := getEnv("DATABASE_PORT", defaultDatabasePort)
	name := getEnv("DATABASE_NAME", defaultDatabaseName)
	user := os.Getenv("DATABASE_USER")
	password := os.Getenv("DATABASE_PASSWORD")

	missing := make([]string, 0)
	if host == "" {
		missing = append(missing, "DATABASE_HOST")
	}
	if port == "" {
		missing = append(missing, "DATABASE_PORT")
	}
	if name == "" {
		missing = append(missing, "DATABASE_NAME")
	}
	if user == "" {
		missing = append(missing, "DATABASE_USER")
	}
	if password == "" {
		missing = append(missing, "DATABASE_PASSWORD")
	}
	if len(missing) > 0 {
		return "", fmt.Errorf("missing database config: %v", missing)
	}

	cfg := mysqldriver.NewConfig()
	cfg.User = user
	cfg.Passwd = password
	cfg.Net = "tcp"
	cfg.Addr = net.JoinHostPort(host, port)
	cfg.DBName = name
	cfg.ParseTime = true
	cfg.Loc = time.UTC
	cfg.Params = map[string]string{
		"charset":   getEnv("DATABASE_CHARSET", defaultDatabaseCharset),
		"collation": getEnv("DATABASE_COLLATION", defaultDatabaseCollation),
	}
	return cfg.FormatDSN(), nil
}

func configurePool(db *sql.DB) {
	db.SetConnMaxLifetime(25 * time.Minute)
	db.SetMaxOpenConns(4)
	db.SetMaxIdleConns(2)
	db.SetConnMaxIdleTime(10 * time.Minute)
}

func getEnv(key, fallback string) string {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}
	return value
}
