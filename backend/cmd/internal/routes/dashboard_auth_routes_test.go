package routes

import (
	"context"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strings"
	"testing"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/discord"
)

type stubDiscordOAuthClient struct {
	token  string
	user   discord.User
	guilds []discord.Guild
}

func (s stubDiscordOAuthClient) Exchange(_ context.Context, code, redirectURI string) (string, error) {
	if code == "" || !strings.HasSuffix(redirectURI, "/auth/discord/callback") {
		return "", assertErr("invalid exchange input")
	}
	return s.token, nil
}

func (s stubDiscordOAuthClient) CurrentUser(_ context.Context, _ string) (discord.User, error) {
	return s.user, nil
}

func (s stubDiscordOAuthClient) CurrentGuilds(_ context.Context, _ string) ([]discord.Guild, error) {
	return s.guilds, nil
}

type assertErr string

func (e assertErr) Error() string {
	return string(e)
}

func TestDashboardLoginRedirectsToDiscordWithStateCookie(t *testing.T) {
	e := echo.New()
	req := httptest.NewRequest(http.MethodGet, "/dashboard/auth/discord/login", nil)
	rec := httptest.NewRecorder()
	handler := NewDashboardAuthHandler(testAuthConfig(nil), stubDiscordOAuthClient{}, NewSessionStore())

	if err := handler.Login(e.NewContext(req, rec)); err != nil {
		t.Fatalf("login returned error: %v", err)
	}

	if rec.Code != http.StatusTemporaryRedirect {
		t.Fatalf("expected redirect status, got %d", rec.Code)
	}
	cookies := rec.Result().Cookies()
	if len(cookies) != 1 || cookies[0].Name != dashboardStateCookie || cookies[0].Value == "" {
		t.Fatalf("expected oauth state cookie, got %#v", cookies)
	}

	location, err := url.Parse(rec.Header().Get("Location"))
	if err != nil {
		t.Fatalf("parse redirect location: %v", err)
	}
	query := location.Query()
	if query.Get("client_id") != "client-id" {
		t.Fatalf("expected client id in redirect, got %q", query.Get("client_id"))
	}
	if query.Get("scope") != "identify guilds" {
		t.Fatalf("expected identify guilds scope, got %q", query.Get("scope"))
	}
	if query.Get("state") != cookies[0].Value {
		t.Fatal("expected redirect state to match state cookie")
	}
}

func TestDashboardCallbackCreatesSessionForManageGuildMember(t *testing.T) {
	e := echo.New()
	req := httptest.NewRequest(http.MethodGet, "/dashboard/auth/discord/callback?code=abc&state=state", nil)
	req.AddCookie(&http.Cookie{Name: dashboardStateCookie, Value: "state"})
	rec := httptest.NewRecorder()
	handler := NewDashboardAuthHandler(
		testAuthConfig(nil),
		stubDiscordOAuthClient{
			token: "token",
			user:  discord.User{ID: "42", Username: "myuu"},
			guilds: []discord.Guild{{
				ID:          "guild-id",
				Name:        "Oficina",
				Permissions: "32",
			}},
		},
		NewSessionStore(),
	)

	if err := handler.Callback(e.NewContext(req, rec)); err != nil {
		t.Fatalf("callback returned error: %v", err)
	}

	if rec.Code != http.StatusTemporaryRedirect {
		t.Fatalf("expected redirect status, got %d", rec.Code)
	}
	if rec.Header().Get("Location") != "/dashboard/birthdays" {
		t.Fatalf("expected birthdays redirect, got %q", rec.Header().Get("Location"))
	}
	foundSessionCookie := false
	for _, cookie := range rec.Result().Cookies() {
		if cookie.Name == dashboardSessionCookie && cookie.Value != "" {
			foundSessionCookie = true
		}
	}
	if !foundSessionCookie {
		t.Fatal("expected dashboard session cookie")
	}
}

func TestDashboardCallbackRejectsMissingManageGuildPermission(t *testing.T) {
	e := echo.New()
	req := httptest.NewRequest(http.MethodGet, "/dashboard/auth/discord/callback?code=abc&state=state", nil)
	req.AddCookie(&http.Cookie{Name: dashboardStateCookie, Value: "state"})
	rec := httptest.NewRecorder()
	handler := NewDashboardAuthHandler(
		testAuthConfig(nil),
		stubDiscordOAuthClient{
			token: "token",
			user:  discord.User{ID: "42", Username: "myuu"},
			guilds: []discord.Guild{{
				ID:          "guild-id",
				Name:        "Oficina",
				Permissions: "0",
			}},
		},
		NewSessionStore(),
	)

	if err := handler.Callback(e.NewContext(req, rec)); err != nil {
		t.Fatalf("callback returned error: %v", err)
	}

	if rec.Header().Get("Location") != "/dashboard/login?error=forbidden" {
		t.Fatalf("expected forbidden redirect, got %q", rec.Header().Get("Location"))
	}
}

func TestDashboardSessionMiddlewareRequiresCSRFForMutations(t *testing.T) {
	e := echo.New()
	sessions := NewSessionStore()
	session, err := sessions.Create(DashboardUser{ID: "42", Username: "myuu"})
	if err != nil {
		t.Fatalf("create session: %v", err)
	}
	handler := NewDashboardAuthHandler(testAuthConfig(nil), stubDiscordOAuthClient{}, sessions)

	req := httptest.NewRequest(http.MethodPost, "/dashboard/api/auth/logout", nil)
	req.AddCookie(&http.Cookie{Name: dashboardSessionCookie, Value: session.ID})
	rec := httptest.NewRecorder()
	next := handler.RequireSession(func(c echo.Context) error {
		return c.NoContent(http.StatusNoContent)
	})

	if err := next(e.NewContext(req, rec)); err != nil {
		t.Fatalf("middleware returned error: %v", err)
	}
	if rec.Code != http.StatusForbidden {
		t.Fatalf("expected CSRF rejection, got %d", rec.Code)
	}
}

func testAuthConfig(missing []string) DashboardAuthConfig {
	return DashboardAuthConfig{
		BaseURL:       "http://localhost:5173/dashboard",
		AuthorizeURL:  "https://discord.com/oauth2/authorize",
		ClientID:      "client-id",
		GuildID:       "guild-id",
		MissingConfig: missing,
	}
}
