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

func TestDashboardRoutesExposeOneGenericChannelMessageSurface(t *testing.T) {
	e := echo.New()
	RegisterDashboardRoutes(e, DashboardRoutesConfig{
		AuthConfig:  testAuthConfig(nil),
		OAuthClient: stubDiscordOAuthClient{},
		Sessions:    NewSessionStore(),
		Tickets:     &fakeTicketRepository{},
		Messages:    &fakeMessageRepository{},
	})
	paths := make(map[string]struct{})
	for _, route := range e.Routes() {
		paths[route.Path] = struct{}{}
	}
	for _, expected := range []string{
		"/channels/:channelID/messages",
		"/channels/:channelID/messages/:messageID/versions",
	} {
		if _, ok := paths[expected]; !ok {
			t.Fatalf("expected route %s", expected)
		}
	}
	for _, removed := range []string{
		"/tickets/:ticketID/messages",
		"/tickets/:ticketID/messages/:messageID/versions",
	} {
		if _, ok := paths[removed]; ok {
			t.Fatalf("did not expect legacy route %s", removed)
		}
	}
}
