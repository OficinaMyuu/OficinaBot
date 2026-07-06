package config

import (
	"testing"
	"time"
)

func TestLoadDatabaseSettingsUsesDefaultsAndCompatiblePoolEnv(t *testing.T) {
	t.Setenv("DATABASE_HOST", "mysql.internal")
	t.Setenv("DATABASE_USER", "app")
	t.Setenv("DATABASE_PASSWORD", "secret")
	t.Setenv("DATABASE_MAX_POOL_SIZE", "5")
	t.Setenv("DATABASE_MIN_IDLE", "8")
	t.Setenv("DATABASE_CONNECTION_TIMEOUT_MS", "2500")

	settings, err := LoadDatabaseSettings()
	if err != nil {
		t.Fatalf("LoadDatabaseSettings() error = %v", err)
	}

	if settings.Host != "mysql.internal" {
		t.Fatalf("Host = %q, want mysql.internal", settings.Host)
	}
	if settings.Port != defaultDatabasePort {
		t.Fatalf("Port = %q, want %q", settings.Port, defaultDatabasePort)
	}
	if settings.Name != defaultDatabaseName {
		t.Fatalf("Name = %q, want %q", settings.Name, defaultDatabaseName)
	}
	if settings.Collation != defaultDatabaseCollation {
		t.Fatalf("Collation = %q, want %q", settings.Collation, defaultDatabaseCollation)
	}
	if settings.MaxOpenConns != 5 {
		t.Fatalf("MaxOpenConns = %d, want 5", settings.MaxOpenConns)
	}
	if settings.MaxIdleConns != 5 {
		t.Fatalf("MaxIdleConns = %d, want capped 5", settings.MaxIdleConns)
	}
	if settings.ConnectionTimeout != 2500*time.Millisecond {
		t.Fatalf("ConnectionTimeout = %s, want 2.5s", settings.ConnectionTimeout)
	}
}

func TestLoadDatabaseSettingsRequiresDatabaseHost(t *testing.T) {
	t.Setenv("DATABASE_USER", "app")
	t.Setenv("DATABASE_PASSWORD", "secret")

	if _, err := LoadDatabaseSettings(); err == nil {
		t.Fatal("LoadDatabaseSettings() error = nil, want missing host error")
	}
}

func TestLoadDatabaseSettingsRejectsInvalidPositiveInteger(t *testing.T) {
	t.Setenv("DATABASE_HOST", "mysql.internal")
	t.Setenv("DATABASE_USER", "app")
	t.Setenv("DATABASE_PASSWORD", "secret")
	t.Setenv("DATABASE_MAX_POOL_SIZE", "0")

	if _, err := LoadDatabaseSettings(); err == nil {
		t.Fatal("LoadDatabaseSettings() error = nil, want positive integer error")
	}
}

func TestLoadDatabaseSettingsRejectsOversizedPool(t *testing.T) {
	t.Setenv("DATABASE_HOST", "mysql.internal")
	t.Setenv("DATABASE_USER", "app")
	t.Setenv("DATABASE_PASSWORD", "secret")
	t.Setenv("DATABASE_MAX_POOL_SIZE", "51")

	if _, err := LoadDatabaseSettings(); err == nil {
		t.Fatal("LoadDatabaseSettings() error = nil, want pool limit error")
	}
}

func TestLoadDatabaseSettingsRejectsOverflowPool(t *testing.T) {
	t.Setenv("DATABASE_HOST", "mysql.internal")
	t.Setenv("DATABASE_USER", "app")
	t.Setenv("DATABASE_PASSWORD", "secret")
	t.Setenv("DATABASE_MAX_POOL_SIZE", "18446744073709551615")

	if _, err := LoadDatabaseSettings(); err == nil {
		t.Fatal("LoadDatabaseSettings() error = nil, want pool parse error")
	}
}

func TestLoadDatabaseSettingsRejectsOversizedDuration(t *testing.T) {
	t.Setenv("DATABASE_HOST", "mysql.internal")
	t.Setenv("DATABASE_USER", "app")
	t.Setenv("DATABASE_PASSWORD", "secret")
	t.Setenv("DATABASE_CONNECTION_TIMEOUT_MS", "600001")

	if _, err := LoadDatabaseSettings(); err == nil {
		t.Fatal("LoadDatabaseSettings() error = nil, want duration limit error")
	}
}

func TestLoadDatabaseSettingsRejectsOverflowDuration(t *testing.T) {
	t.Setenv("DATABASE_HOST", "mysql.internal")
	t.Setenv("DATABASE_USER", "app")
	t.Setenv("DATABASE_PASSWORD", "secret")
	t.Setenv("DATABASE_CONNECTION_TIMEOUT_MS", "18446744073709551615")

	if _, err := LoadDatabaseSettings(); err == nil {
		t.Fatal("LoadDatabaseSettings() error = nil, want duration overflow limit error")
	}
}
