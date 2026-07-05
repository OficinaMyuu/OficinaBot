package config

import (
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"
)

const (
	defaultDatabasePort      = "3306"
	defaultDatabaseName      = "oficina_services"
	defaultDatabaseCollation = "utf8mb4_unicode_ci"
)

type DatabaseSettings struct {
	Host              string
	Port              string
	Name              string
	User              string
	Password          string
	Collation         string
	MaxOpenConns      int
	MaxIdleConns      int
	ConnectionTimeout time.Duration
	ValidationTimeout time.Duration
	ConnMaxIdleTime   time.Duration
	ConnMaxLifetime   time.Duration
}

func LoadDatabaseSettings() (DatabaseSettings, error) {
	host, err := requiredEnv("DATABASE_HOST")
	if err != nil {
		return DatabaseSettings{}, err
	}
	user, err := requiredEnv("DATABASE_USER")
	if err != nil {
		return DatabaseSettings{}, err
	}
	password, err := requiredEnv("DATABASE_PASSWORD")
	if err != nil {
		return DatabaseSettings{}, err
	}

	maxOpen, err := positiveIntEnv("DATABASE_MAX_POOL_SIZE", 3)
	if err != nil {
		return DatabaseSettings{}, err
	}
	minIdle, err := positiveIntEnv("DATABASE_MIN_IDLE", 1)
	if err != nil {
		return DatabaseSettings{}, err
	}
	if minIdle > maxOpen {
		minIdle = maxOpen
	}

	connectionTimeout, err := positiveDurationMillisEnv("DATABASE_CONNECTION_TIMEOUT_MS", 10_000)
	if err != nil {
		return DatabaseSettings{}, err
	}
	validationTimeout, err := positiveDurationMillisEnv("DATABASE_VALIDATION_TIMEOUT_MS", 5_000)
	if err != nil {
		return DatabaseSettings{}, err
	}
	idleTimeout, err := positiveDurationMillisEnv("DATABASE_IDLE_TIMEOUT_MS", 600_000)
	if err != nil {
		return DatabaseSettings{}, err
	}
	maxLifetime, err := positiveDurationMillisEnv("DATABASE_MAX_LIFETIME_MS", 1_500_000)
	if err != nil {
		return DatabaseSettings{}, err
	}

	return DatabaseSettings{
		Host:              host,
		Port:              env("DATABASE_PORT", defaultDatabasePort),
		Name:              env("DATABASE_NAME", defaultDatabaseName),
		User:              user,
		Password:          password,
		Collation:         env("DATABASE_COLLATION", defaultDatabaseCollation),
		MaxOpenConns:      maxOpen,
		MaxIdleConns:      minIdle,
		ConnectionTimeout: connectionTimeout,
		ValidationTimeout: validationTimeout,
		ConnMaxIdleTime:   idleTimeout,
		ConnMaxLifetime:   maxLifetime,
	}, nil
}

func requiredEnv(key string) (string, error) {
	value := strings.TrimSpace(os.Getenv(key))
	if value == "" {
		return "", fmt.Errorf("missing required database environment variable: %s", key)
	}
	return value, nil
}

func env(key string, fallback string) string {
	value := strings.TrimSpace(os.Getenv(key))
	if value == "" {
		return fallback
	}
	return value
}

func positiveIntEnv(key string, fallback int) (int, error) {
	value, err := positiveInt64Env(key, int64(fallback))
	if err != nil {
		return 0, err
	}
	if value > int64(^uint(0)>>1) {
		return 0, fmt.Errorf("%s must be less than or equal to max int", key)
	}
	return int(value), nil
}

func positiveDurationMillisEnv(key string, fallback int64) (time.Duration, error) {
	value, err := positiveInt64Env(key, fallback)
	if err != nil {
		return 0, err
	}
	return time.Duration(value) * time.Millisecond, nil
}

func positiveInt64Env(key string, fallback int64) (int64, error) {
	raw := strings.TrimSpace(os.Getenv(key))
	if raw == "" {
		return fallback, nil
	}
	value, err := strconv.ParseInt(raw, 10, 64)
	if err != nil {
		return 0, fmt.Errorf("%s must be a number: %w", key, err)
	}
	if value <= 0 {
		return 0, fmt.Errorf("%s must be positive", key)
	}
	return value, nil
}
