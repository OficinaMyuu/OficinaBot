package handler

import (
	"context"
	"net/http"
	"net/http/httptest"
	"net/url"
	"strings"
	"testing"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/infrastructure/discord"
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
	req := httptest.NewRequest(http.MethodGet, "/auth/discord/login?return_to=https://oficinamyuu.com.br/dashboard/birthdays", nil)
	rec := httptest.NewRecorder()
	handler := NewDashboardAuthHandler(testAuthConfig(nil), stubDiscordOAuthClient{}, NewSessionStore())

	if err := handler.Login(e.NewContext(req, rec)); err != nil {
		t.Fatalf("login returned error: %v", err)
	}

	if rec.Code != http.StatusTemporaryRedirect {
		t.Fatalf("expected redirect status, got %d", rec.Code)
	}
	cookies := rec.Result().Cookies()
	if len(cookies) != 2 {
		t.Fatalf("expected oauth state and return cookies, got %#v", cookies)
	}
	state := cookieValue(cookies, dashboardStateCookie)
	if state == "" {
		t.Fatalf("expected oauth state cookie, got %#v", cookies)
	}
	if got := cookieValue(cookies, dashboardReturnToCookie); got != "https://oficinamyuu.com.br/dashboard/birthdays" {
		t.Fatalf("expected return cookie, got %q", got)
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
	if query.Get("redirect_uri") != "http://localhost:8080/auth/discord/callback" {
		t.Fatalf("expected API callback redirect URI, got %q", query.Get("redirect_uri"))
	}
	if query.Get("state") != state {
		t.Fatal("expected redirect state to match state cookie")
	}
}

func TestDashboardLoginAllowsCORSAllowedOriginsReturnTo(t *testing.T) {
	cfg := testAuthConfig(nil)
	cfg.CORSAllowedOrigins = []string{"https://dev.oficinamyuu.com.br", "http://localhost:5173"}

	tests := []struct {
		name         string
		returnTo     string
		wantCookieTo string
	}{
		{
			name:         "allowed dev origin",
			returnTo:     "https://dev.oficinamyuu.com.br/dashboard/birthdays",
			wantCookieTo: "https://dev.oficinamyuu.com.br/dashboard/birthdays",
		},
		{
			name:         "allowed localhost origin",
			returnTo:     "http://localhost:5173/dashboard",
			wantCookieTo: "http://localhost:5173/dashboard",
		},
		{
			name:         "disallowed origin falls back to default",
			returnTo:     "https://evil.com/dashboard",
			wantCookieTo: "https://oficinamyuu.com.br/dashboard",
		},
	}

	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			e := echo.New()
			req := httptest.NewRequest(http.MethodGet, "/auth/discord/login?return_to="+url.QueryEscape(tc.returnTo), nil)
			rec := httptest.NewRecorder()
			handler := NewDashboardAuthHandler(cfg, stubDiscordOAuthClient{}, NewSessionStore())

			if err := handler.Login(e.NewContext(req, rec)); err != nil {
				t.Fatalf("login returned error: %v", err)
			}
			if got := cookieValue(rec.Result().Cookies(), dashboardReturnToCookie); got != tc.wantCookieTo {
				t.Fatalf("expected return cookie %q, got %q", tc.wantCookieTo, got)
			}
		})
	}
}

func TestDashboardCallbackCreatesSessionForManageGuildMember(t *testing.T) {
	e := echo.New()
	req := httptest.NewRequest(http.MethodGet, "/auth/discord/callback?code=abc&state=state", nil)
	req.AddCookie(&http.Cookie{Name: dashboardStateCookie, Value: "state"})
	req.AddCookie(&http.Cookie{Name: dashboardReturnToCookie, Value: "https://oficinamyuu.com.br/dashboard/birthdays"})
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
	if rec.Header().Get("Location") != "https://oficinamyuu.com.br/dashboard/birthdays" {
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
	req := httptest.NewRequest(http.MethodGet, "/auth/discord/callback?code=abc&state=state", nil)
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

	if rec.Header().Get("Location") != "https://oficinamyuu.com.br/dashboard/login?error=forbidden" {
		t.Fatalf("expected forbidden redirect, got %q", rec.Header().Get("Location"))
	}
}

func TestDashboardReturnToRejectsExternalOrigins(t *testing.T) {
	handler := NewDashboardAuthHandler(testAuthConfig(nil), stubDiscordOAuthClient{}, NewSessionStore())

	got := handler.safeReturnTo("https://evil.example/dashboard")

	if got != "https://oficinamyuu.com.br/dashboard" {
		t.Fatalf("expected default dashboard return URL, got %q", got)
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

	req := httptest.NewRequest(http.MethodPost, "/auth/logout", nil)
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

func TestSessionStoreCanReloadRepositoryBackedSession(t *testing.T) {
	repository := newFakeSessionRepository()
	created, err := NewSessionStore(repository).Create(DashboardUser{ID: "42", Username: "myuu"})
	if err != nil {
		t.Fatalf("create session: %v", err)
	}

	found, ok := NewSessionStore(repository).Find(created.ID)

	if !ok {
		t.Fatal("expected session to survive store recreation")
	}
	if found.User.ID != "42" || found.CSRFToken == "" || found.ID != created.ID {
		t.Fatalf("unexpected reloaded session: %+v", found)
	}
}

func TestDashboardMeReturnsSnakeCaseSessionFields(t *testing.T) {
	e := echo.New()
	sessions := NewSessionStore()
	globalName := "Oficina Myuu"
	avatar := "https://cdn.example/avatar.png"
	guildIcon := "https://cdn.example/icon.png"
	session, err := sessions.Create(DashboardUser{
		ID:           "42",
		Username:     "myuu",
		GlobalName:   &globalName,
		AvatarURL:    &avatar,
		GuildName:    "Oficina",
		GuildIconURL: &guildIcon,
		Permissions:  "32",
	})
	if err != nil {
		t.Fatalf("create session: %v", err)
	}
	handler := NewDashboardAuthHandler(testAuthConfig(nil), stubDiscordOAuthClient{}, sessions)
	req := httptest.NewRequest(http.MethodGet, "/auth/me", nil)
	req.AddCookie(&http.Cookie{Name: dashboardSessionCookie, Value: session.ID})
	rec := httptest.NewRecorder()

	if err := handler.Me(e.NewContext(req, rec)); err != nil {
		t.Fatalf("me returned error: %v", err)
	}

	body := rec.Body.String()
	for _, expected := range []string{`"csrf_token":`, `"global_name":`, `"avatar_url":`, `"guild_name":`, `"guild_icon_url":`} {
		if !strings.Contains(body, expected) {
			t.Fatalf("expected %s in response, got %s", expected, body)
		}
	}
	for _, unexpected := range []string{`"csrfToken":`, `"globalName":`, `"avatarUrl":`, `"guildName":`, `"guildIconUrl":`} {
		if strings.Contains(body, unexpected) {
			t.Fatalf("did not expect %s in response, got %s", unexpected, body)
		}
	}
}

func testAuthConfig(missing []string) DashboardAuthConfig {
	return DashboardAuthConfig{
		PublicAPIBaseURL: "http://localhost:8080",
		FrontendBaseURL:  "https://oficinamyuu.com.br",
		AuthorizeURL:     "https://discord.com/oauth2/authorize",
		ClientID:         "client-id",
		GuildID:          "guild-id",
		MissingConfig:    missing,
	}
}

func cookieValue(cookies []*http.Cookie, name string) string {
	for _, cookie := range cookies {
		if cookie.Name == name {
			return cookie.Value
		}
	}
	return ""
}

type fakeSessionRepository struct {
	sessions map[string]DashboardSession
}

func newFakeSessionRepository() *fakeSessionRepository {
	return &fakeSessionRepository{sessions: make(map[string]DashboardSession)}
}

func (f *fakeSessionRepository) Save(_ context.Context, sessionIDHash string, session DashboardSession) error {
	f.sessions[sessionIDHash] = session
	return nil
}

func (f *fakeSessionRepository) Find(_ context.Context, sessionIDHash string) (DashboardSession, error) {
	session, ok := f.sessions[sessionIDHash]
	if !ok {
		return DashboardSession{}, assertErr("missing session")
	}
	return session, nil
}

func (f *fakeSessionRepository) Delete(_ context.Context, sessionIDHash string) error {
	delete(f.sessions, sessionIDHash)
	return nil
}

func (f *fakeSessionRepository) DeleteExpired(_ context.Context, _ int64) error {
	return nil
}
