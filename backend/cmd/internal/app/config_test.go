package app

import "testing"

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
