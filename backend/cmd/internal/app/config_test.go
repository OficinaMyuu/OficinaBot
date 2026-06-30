package app

import (
	"os"
	"testing"

	mysqldriver "github.com/go-sql-driver/mysql"
)

func TestConfigValidateRuntimeRequiresBaseSettings(t *testing.T) {
	cfg := Config{}

	if err := cfg.ValidateRuntime(); err == nil {
		t.Fatal("expected missing runtime config error")
	}
}

func TestConfigValidateRuntimeAcceptsBaseSettingsWithoutDiscordOAuth(t *testing.T) {
	cfg := baseRuntimeConfig()
	cfg.DiscordBotToken = "bot-token"
	cfg.SessionSecret = "secret"
	cfg.SessionTTL = DefaultSessionTTL

	if err := cfg.ValidateRuntime(); err != nil {
		t.Fatalf("expected valid runtime config, got %v", err)
	}
}

func TestConfigValidateRuntimeRejectsMissingDatabaseSettings(t *testing.T) {
	cfg := Config{
		DiscordBotToken: "bot-token",
		SessionSecret:   "secret",
		SessionTTL:      DefaultSessionTTL,
	}

	if err := cfg.ValidateRuntime(); err == nil {
		t.Fatal("expected missing database config error")
	}
}

func TestConfigValidateRuntimeRejectsIncompleteDiscordOAuthSettings(t *testing.T) {
	cfg := baseRuntimeConfig()
	cfg.DiscordClientID = "client-id"
	cfg.DiscordBotToken = "bot-token"
	cfg.SessionSecret = "secret"
	cfg.SessionTTL = DefaultSessionTTL

	if err := cfg.ValidateRuntime(); err == nil {
		t.Fatal("expected incomplete Discord OAuth config error")
	}
}

func TestConfigValidateRuntimeAcceptsCompleteDiscordOAuthSettings(t *testing.T) {
	cfg := baseRuntimeConfig()
	cfg.DiscordClientID = "client-id"
	cfg.DiscordClientSecret = "client-secret"
	cfg.DiscordRedirectURL = "http://localhost/callback"
	cfg.DiscordBotToken = "bot-token"
	cfg.OwnerDiscordID = "100"
	cfg.SessionSecret = "secret"
	cfg.SessionTTL = DefaultSessionTTL

	if err := cfg.ValidateRuntime(); err != nil {
		t.Fatalf("expected valid runtime config, got %v", err)
	}
}

func TestDatabaseConfigBuildsMySQLDSN(t *testing.T) {
	cfg := Config{
		DatabaseHost:     "mysql.internal",
		DatabasePort:     "3306",
		DatabaseName:     "oficina_services",
		DatabaseUser:     "oficina",
		DatabasePassword: "secret",
	}

	dbConfig, err := cfg.DatabaseConfig()
	if err != nil {
		t.Fatalf("build database config: %v", err)
	}

	parsed, err := mysqldriver.ParseDSN(dbConfig.DSN)
	if err != nil {
		t.Fatalf("parse generated DSN: %v", err)
	}
	if parsed.Addr != "mysql.internal:3306" {
		t.Fatalf("expected mysql.internal:3306, got %q", parsed.Addr)
	}
	if parsed.DBName != "oficina_services" {
		t.Fatalf("expected oficina_services database, got %q", parsed.DBName)
	}
	if !parsed.ParseTime {
		t.Fatal("expected parseTime to be enabled")
	}
}

func TestGetBoolEnvFallsBackForInvalidValues(t *testing.T) {
	t.Setenv("SESSION_COOKIE_SECURE", "definitely")

	if got := getBoolEnv("SESSION_COOKIE_SECURE", true); !got {
		t.Fatal("expected invalid bool to fall back to true")
	}
}

func TestLoadConfigReadsHttpSafeguardSettings(t *testing.T) {
	workingDir, err := os.Getwd()
	if err != nil {
		t.Fatalf("get working dir: %v", err)
	}
	if err := os.Chdir(t.TempDir()); err != nil {
		t.Fatalf("chdir temp dir: %v", err)
	}
	t.Cleanup(func() {
		if err := os.Chdir(workingDir); err != nil {
			t.Fatalf("restore working dir: %v", err)
		}
	})
	if err := os.WriteFile(".env", nil, 0644); err != nil {
		t.Fatalf("write test env file: %v", err)
	}
	t.Setenv("DATABASE_HOST", "mysql.internal")
	t.Setenv("DATABASE_USER", "oficina")
	t.Setenv("DATABASE_PASSWORD", "secret")
	t.Setenv("BODY_LIMIT", "2M")
	t.Setenv("SESSION_COOKIE_SECURE", "false")

	cfg, err := LoadConfig()
	if err != nil {
		t.Fatalf("load config: %v", err)
	}

	if cfg.BodyLimit != "2M" {
		t.Fatalf("expected body limit from env, got %q", cfg.BodyLimit)
	}
	if cfg.DatabaseHost != "mysql.internal" {
		t.Fatalf("expected database host from env, got %q", cfg.DatabaseHost)
	}
	if cfg.DatabaseName != DefaultDatabaseName {
		t.Fatalf("expected default database name, got %q", cfg.DatabaseName)
	}
	if cfg.SessionCookieSecure {
		t.Fatal("expected session cookie secure to be false")
	}
}

func baseRuntimeConfig() Config {
	return Config{
		DatabaseHost:     "mysql.internal",
		DatabasePort:     "3306",
		DatabaseName:     "oficina_services",
		DatabaseUser:     "oficina",
		DatabasePassword: "secret",
	}
}
