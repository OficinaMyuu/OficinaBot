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
	maxDatabasePoolSize      = 50
	maxDatabaseTimeout       = 10 * time.Minute
	maxDatabaseLifetime      = 24 * time.Hour
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

	maxOpen, err := boundedPositiveIntEnv("DATABASE_MAX_POOL_SIZE", 3, maxDatabasePoolSize)
	if err != nil {
		return DatabaseSettings{}, err
	}
	minIdle, err := boundedPositiveIntEnv("DATABASE_MIN_IDLE", 1, maxDatabasePoolSize)
	if err != nil {
		return DatabaseSettings{}, err
	}
	if minIdle > maxOpen {
		minIdle = maxOpen
	}

	connectionTimeout, err := boundedPositiveDurationMillisEnv("DATABASE_CONNECTION_TIMEOUT_MS", 10_000, maxDatabaseTimeout)
	if err != nil {
		return DatabaseSettings{}, err
	}
	validationTimeout, err := boundedPositiveDurationMillisEnv("DATABASE_VALIDATION_TIMEOUT_MS", 5_000, maxDatabaseTimeout)
	if err != nil {
		return DatabaseSettings{}, err
	}
	idleTimeout, err := boundedPositiveDurationMillisEnv("DATABASE_IDLE_TIMEOUT_MS", 600_000, maxDatabaseLifetime)
	if err != nil {
		return DatabaseSettings{}, err
	}
	maxLifetime, err := boundedPositiveDurationMillisEnv("DATABASE_MAX_LIFETIME_MS", 1_500_000, maxDatabaseLifetime)
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

func boundedPositiveIntEnv(key string, fallback int, maxValue int) (int, error) {
	if fallback <= 0 || fallback > maxValue {
		return 0, fmt.Errorf("invalid fallback for %s", key)
	}

	value, err := boundedPositiveUintEnv(key, uint64(fallback), uint64(maxValue))
	if err != nil {
		return 0, err
	}
	return int(value), nil
}

func boundedPositiveDurationMillisEnv(key string, fallbackMillis uint64, maxValue time.Duration) (time.Duration, error) {
	if maxValue <= 0 {
		return 0, fmt.Errorf("invalid duration limit for %s", key)
	}

	maxMillis := uint64(maxValue / time.Millisecond)
	if fallbackMillis == 0 || fallbackMillis > maxMillis {
		return 0, fmt.Errorf("invalid fallback for %s", key)
	}

	value, err := boundedPositiveUintEnv(key, fallbackMillis, maxMillis)
	if err != nil {
		return 0, err
	}
	return time.Duration(value) * time.Millisecond, nil
}

func boundedPositiveUintEnv(key string, fallback uint64, maxValue uint64) (uint64, error) {
	if fallback == 0 || fallback > maxValue {
		return 0, fmt.Errorf("invalid fallback for %s", key)
	}

	raw := strings.TrimSpace(os.Getenv(key))
	if raw == "" {
		return fallback, nil
	}
	value, err := strconv.ParseUint(raw, 10, 64)
	if err != nil {
		return 0, fmt.Errorf("%s must be a positive integer: %w", key, err)
	}
	if value == 0 {
		return 0, fmt.Errorf("%s must be positive", key)
	}
	if value > maxValue {
		return 0, fmt.Errorf("%s must be less than or equal to %d", key, maxValue)
	}
	return value, nil
}
