package database

import (
	"context"
	"database/sql"
	"fmt"
	"net"

	"github.com/go-sql-driver/mysql"

	"oficina-registrar/internal/config"
)

func Open(ctx context.Context, settings config.DatabaseSettings) (*sql.DB, error) {
	mysqlConfig := mysql.NewConfig()
	mysqlConfig.User = settings.User
	mysqlConfig.Passwd = settings.Password
	mysqlConfig.Net = "tcp"
	mysqlConfig.Addr = net.JoinHostPort(settings.Host, settings.Port)
	mysqlConfig.DBName = settings.Name
	mysqlConfig.Collation = settings.Collation
	mysqlConfig.ParseTime = true
	mysqlConfig.Timeout = settings.ConnectionTimeout
	mysqlConfig.ReadTimeout = settings.ConnectionTimeout
	mysqlConfig.WriteTimeout = settings.ConnectionTimeout

	db, err := sql.Open("mysql", mysqlConfig.FormatDSN())
	if err != nil {
		return nil, fmt.Errorf("create mysql handle: %w", err)
	}
	db.SetMaxOpenConns(settings.MaxOpenConns)
	db.SetMaxIdleConns(settings.MaxIdleConns)
	db.SetConnMaxIdleTime(settings.ConnMaxIdleTime)
	db.SetConnMaxLifetime(settings.ConnMaxLifetime)

	pingCtx, cancel := context.WithTimeout(ctx, settings.ValidationTimeout)
	defer cancel()
	if err := db.PingContext(pingCtx); err != nil {
		db.Close()
		return nil, fmt.Errorf("ping mysql: %w", err)
	}

	return db, nil
}
