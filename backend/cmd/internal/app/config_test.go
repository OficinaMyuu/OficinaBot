package app

import (
	"os"
	"testing"
)

func TestConfigValidateAuthRequiresOAuthSettings(t *testing.T) {
	cfg := Config{}

	if err := cfg.ValidateAuth(); err == nil {
		t.Fatal("expected missing auth config error")
	}
}

func TestConfigValidateAuthAcceptsRequiredSettings(t *testing.T) {
	cfg := Config{
		DiscordClientID:     "client-id",
		DiscordClientSecret: "client-secret",
		DiscordRedirectURL:  "http://localhost/callback",
		DiscordBotToken:     "bot-token",
		OwnerDiscordID:      "100",
		SessionSecret:       "secret",
		SessionTTL:          DefaultSessionTTL,
	}

	if err := cfg.ValidateAuth(); err != nil {
		t.Fatalf("expected valid auth config, got %v", err)
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
	t.Setenv("FRONTEND_ORIGIN", "https://oficina.test")
	t.Setenv("BODY_LIMIT", "2M")
	t.Setenv("SESSION_COOKIE_SECURE", "false")

	cfg, err := LoadConfig()
	if err != nil {
		t.Fatalf("load config: %v", err)
	}

	if cfg.FrontendOrigin != "https://oficina.test" {
		t.Fatalf("expected frontend origin from env, got %q", cfg.FrontendOrigin)
	}
	if cfg.BodyLimit != "2M" {
		t.Fatalf("expected body limit from env, got %q", cfg.BodyLimit)
	}
	if cfg.SessionCookieSecure {
		t.Fatal("expected session cookie secure to be false")
	}
}
