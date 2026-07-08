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
	t.Setenv("PUBLIC_API_BASE_URL", "https://api.oficinamyuu.com.br/")
	t.Setenv("FRONTEND_BASE_URL", "https://oficinamyuu.com.br/")
	t.Setenv("CORS_ALLOWED_ORIGINS", "https://oficinamyuu.com.br, https://www.oficinamyuu.com.br")

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
	if cfg.Dashboard.PublicAPIBaseURL != "https://api.oficinamyuu.com.br" {
		t.Fatalf("expected trimmed public API base URL, got %q", cfg.Dashboard.PublicAPIBaseURL)
	}
	if cfg.Dashboard.FrontendBaseURL != "https://oficinamyuu.com.br" {
		t.Fatalf("expected trimmed frontend base URL, got %q", cfg.Dashboard.FrontendBaseURL)
	}
	expectedOrigins := []string{"https://oficinamyuu.com.br", "https://www.oficinamyuu.com.br"}
	for i, expected := range expectedOrigins {
		if cfg.Dashboard.CORSAllowedOrigins[i] != expected {
			t.Fatalf("expected CORS origin %q at %d, got %q", expected, i, cfg.Dashboard.CORSAllowedOrigins[i])
		}
	}
}

func TestDashboardConfigDefaultsCORSOriginsFromFrontendBaseURL(t *testing.T) {
	cfg := DashboardConfig{FrontendBaseURL: "https://oficinamyuu.com.br/dashboard"}

	origins := cfg.AllowedCORSOrigins()

	if len(origins) != 1 || origins[0] != "https://oficinamyuu.com.br" {
		t.Fatalf("expected frontend origin, got %#v", origins)
	}
}

func baseRuntimeConfig() Config {
	return Config{
		Address:   DefaultAddress,
		BodyLimit: DefaultBodyLimit,
	}
}
