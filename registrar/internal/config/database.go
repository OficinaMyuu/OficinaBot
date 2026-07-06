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

	connectionTimeout, err := boundedPositiveDurationMillisEnv("DATABASE_CONNECTION_TIMEOUT_MS", 10*time.Second, maxDatabaseTimeout)
	if err != nil {
		return DatabaseSettings{}, err
	}
	validationTimeout, err := boundedPositiveDurationMillisEnv("DATABASE_VALIDATION_TIMEOUT_MS", 5*time.Second, maxDatabaseTimeout)
	if err != nil {
		return DatabaseSettings{}, err
	}
	idleTimeout, err := boundedPositiveDurationMillisEnv("DATABASE_IDLE_TIMEOUT_MS", 10*time.Minute, maxDatabaseLifetime)
	if err != nil {
		return DatabaseSettings{}, err
	}
	maxLifetime, err := boundedPositiveDurationMillisEnv("DATABASE_MAX_LIFETIME_MS", 25*time.Minute, maxDatabaseLifetime)
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

	raw := strings.TrimSpace(os.Getenv(key))
	if raw == "" {
		return fallback, nil
	}

	value, err := strconv.Atoi(raw)
	if err != nil {
		return 0, fmt.Errorf("%s must be a positive integer: %w", key, err)
	}
	if value <= 0 {
		return 0, fmt.Errorf("%s must be positive", key)
	}
	if value > maxValue {
		return 0, fmt.Errorf("%s must be less than or equal to %d", key, maxValue)
	}
	return value, nil
}

func boundedPositiveDurationMillisEnv(key string, fallback time.Duration, maxValue time.Duration) (time.Duration, error) {
	if maxValue <= 0 {
		return 0, fmt.Errorf("invalid duration limit for %s", key)
	}
	if fallback <= 0 || fallback > maxValue {
		return 0, fmt.Errorf("invalid fallback for %s", key)
	}

	raw := strings.TrimSpace(os.Getenv(key))
	if raw == "" {
		return fallback, nil
	}
	if !isDecimalDigits(raw) {
		return 0, fmt.Errorf("%s must be a positive integer", key)
	}

	value, err := time.ParseDuration(raw + "ms")
	if err != nil {
		return 0, fmt.Errorf("%s must be a valid millisecond duration: %w", key, err)
	}
	if value <= 0 {
		return 0, fmt.Errorf("%s must be positive", key)
	}
	if value > maxValue {
		return 0, fmt.Errorf("%s must be less than or equal to %s", key, maxValue)
	}
	return value, nil
}

func isDecimalDigits(value string) bool {
	for _, char := range value {
		if char < '0' || char > '9' {
			return false
		}
	}
	return value != ""
}
