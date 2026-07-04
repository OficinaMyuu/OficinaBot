package app

import (
	"os"
	"testing"
)

func TestConfigValidateRuntimeRejectsMissingAddress(t *testing.T) {
	cfg := Config{BodyLimit: DefaultBodyLimit}

	if err := cfg.ValidateRuntime(); err == nil {
		t.Fatal("expected missing address error")
	}
}

func TestConfigValidateRuntimeRejectsMissingBodyLimit(t *testing.T) {
	cfg := Config{Address: DefaultAddress}

	if err := cfg.ValidateRuntime(); err == nil {
		t.Fatal("expected missing body limit error")
	}
}

func TestConfigValidateRuntimeAcceptsBaseSettings(t *testing.T) {
	cfg := baseRuntimeConfig()

	if err := cfg.ValidateRuntime(); err != nil {
		t.Fatalf("expected valid runtime config, got %v", err)
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
	t.Setenv("ADDRESS", ":9090")
	t.Setenv("BODY_LIMIT", "2M")

	cfg, err := LoadConfig()
	if err != nil {
		t.Fatalf("load config: %v", err)
	}

	if cfg.BodyLimit != "2M" {
		t.Fatalf("expected body limit from env, got %q", cfg.BodyLimit)
	}
	if cfg.Address != ":9090" {
		t.Fatalf("expected address from env, got %q", cfg.Address)
	}
}

func baseRuntimeConfig() Config {
	return Config{
		Address:   DefaultAddress,
		BodyLimit: DefaultBodyLimit,
	}
}
