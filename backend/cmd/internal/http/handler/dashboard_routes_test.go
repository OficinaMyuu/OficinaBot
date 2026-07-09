package handler

import (
	"net/http"
	"net/http/httptest"
	"net/url"
	"testing"

	"github.com/labstack/echo/v4"
)

func TestDashboardRoutesExposeAPIOnlyPaths(t *testing.T) {
	e := echo.New()
	RegisterDashboardRoutes(e, DashboardRoutesConfig{
		AuthConfig:  testAuthConfig(nil),
		OAuthClient: stubDiscordOAuthClient{},
		Sessions:    NewSessionStore(),
		Birthdays:   &fakeBirthdayRepository{},
	})

	req := httptest.NewRequest(http.MethodGet, "/auth/discord/login", nil)
	rec := httptest.NewRecorder()

	e.ServeHTTP(rec, req)

	if rec.Code != http.StatusTemporaryRedirect {
		t.Fatalf("expected auth route redirect, got %d", rec.Code)
	}
	if _, err := url.Parse(rec.Header().Get("Location")); err != nil {
		t.Fatalf("expected valid redirect location: %v", err)
	}
}

func TestDashboardRoutesDoNotServeFrontendAssets(t *testing.T) {
	e := echo.New()
	RegisterDashboardRoutes(e, DashboardRoutesConfig{
		AuthConfig:  testAuthConfig(nil),
		OAuthClient: stubDiscordOAuthClient{},
		Sessions:    NewSessionStore(),
		Birthdays:   &fakeBirthdayRepository{},
	})

	req := httptest.NewRequest(http.MethodGet, "/dashboard/assets/index-test.css", nil)
	rec := httptest.NewRecorder()

	e.ServeHTTP(rec, req)

	if rec.Code != http.StatusNotFound {
		t.Fatalf("expected frontend asset route to be absent from backend, got %d", rec.Code)
	}
}
