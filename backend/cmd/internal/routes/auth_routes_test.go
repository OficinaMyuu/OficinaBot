package routes

import (
	"context"
	"errors"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/auth"
	"oficina-img/internal/repository"
)

func TestAuthHandlerStartsDiscordLogin(t *testing.T) {
	e := echo.New()
	authService := &stubAdminAuthService{startURL: "https://discord.test/auth", state: "state"}
	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, "/api/auth/discord/start", nil)

	err := newTestAuthHandler(authService).StartDiscordLogin(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusFound {
		t.Fatalf("expected status %d, got %d", http.StatusFound, rec.Code)
	}
	if rec.Header().Get(echo.HeaderLocation) != "https://discord.test/auth" {
		t.Fatalf("unexpected redirect %q", rec.Header().Get(echo.HeaderLocation))
	}
	assertCookie(t, rec, oauthStateCookieName, "state")
}

func TestAuthHandlerCompletesDiscordLogin(t *testing.T) {
	e := echo.New()
	authService := &stubAdminAuthService{sessionToken: "session"}
	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, "/api/auth/discord/callback?code=code&state=state", nil)
	req.AddCookie(&http.Cookie{Name: oauthStateCookieName, Value: "state"})

	err := newTestAuthHandler(authService).CompleteDiscordLogin(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusFound {
		t.Fatalf("expected status %d, got %d", http.StatusFound, rec.Code)
	}
	if rec.Header().Get(echo.HeaderLocation) != "http://frontend.test" {
		t.Fatalf("unexpected redirect %q", rec.Header().Get(echo.HeaderLocation))
	}
	if authService.completedCode != "code" {
		t.Fatalf("expected code to be passed to service")
	}
	assertCookie(t, rec, "session", "session")
}

func TestAuthHandlerRejectsUnauthorizedOAuthUser(t *testing.T) {
	e := echo.New()
	authService := &stubAdminAuthService{completeErr: auth.ErrUnauthorizedAdmin}
	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, "/api/auth/discord/callback?code=code&state=state", nil)
	req.AddCookie(&http.Cookie{Name: oauthStateCookieName, Value: "state"})

	err := newTestAuthHandler(authService).CompleteDiscordLogin(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusForbidden {
		t.Fatalf("expected status %d, got %d", http.StatusForbidden, rec.Code)
	}
}

func TestAuthHandlerReturnsCurrentUser(t *testing.T) {
	e := echo.New()
	authService := &stubAdminAuthService{
		currentUser: &repository.User{DiscordID: "100", Username: "Leonardo"},
		owner:       true,
	}
	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, "/api/auth/me", nil)
	req.AddCookie(&http.Cookie{Name: "session", Value: "token"})

	err := newTestAuthHandler(authService).CurrentUser(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusOK {
		t.Fatalf("expected status %d, got %d", http.StatusOK, rec.Code)
	}
	if !strings.Contains(rec.Body.String(), `"is_owner":true`) {
		t.Fatalf("expected owner response, got %s", rec.Body.String())
	}
}

func TestAuthHandlerLogsOut(t *testing.T) {
	e := echo.New()
	authService := &stubAdminAuthService{}
	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodPost, "/api/auth/logout", nil)
	req.AddCookie(&http.Cookie{Name: "session", Value: "token"})

	err := newTestAuthHandler(authService).Logout(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusNoContent {
		t.Fatalf("expected status %d, got %d", http.StatusNoContent, rec.Code)
	}
	if authService.loggedOutToken != "token" {
		t.Fatalf("expected token logout, got %q", authService.loggedOutToken)
	}
}

func newTestAuthHandler(authService *stubAdminAuthService) *AuthHandler {
	return NewAuthHandler(authService, AuthCookieConfig{
		SessionName:         "session",
		Secure:              true,
		FrontendRedirectURL: "http://frontend.test",
	})
}

type stubAdminAuthService struct {
	startURL       string
	state          string
	sessionToken   string
	completeErr    error
	completedCode  string
	currentUser    *repository.User
	currentUserErr error
	loggedOutToken string
	owner          bool
}

func (s *stubAdminAuthService) StartURL() (string, string, error) {
	return s.startURL, s.state, nil
}

func (s *stubAdminAuthService) CompleteOAuth(_ context.Context, code, _, _ string) (string, *repository.User, error) {
	s.completedCode = code
	if s.completeErr != nil {
		return "", nil, s.completeErr
	}
	return s.sessionToken, &repository.User{DiscordID: "100", Username: "Leonardo"}, nil
}

func (s *stubAdminAuthService) CurrentUser(_ context.Context, _ string) (*repository.User, error) {
	if s.currentUserErr != nil {
		return nil, s.currentUserErr
	}
	if s.currentUser == nil {
		return nil, errors.New("missing user")
	}
	return s.currentUser, nil
}

func (s *stubAdminAuthService) Logout(_ context.Context, sessionToken string) error {
	s.loggedOutToken = sessionToken
	return nil
}

func (s *stubAdminAuthService) IsOwner(_ *repository.User) bool {
	return s.owner
}

func assertCookie(t *testing.T, rec *httptest.ResponseRecorder, name, value string) {
	t.Helper()

	for _, cookie := range rec.Result().Cookies() {
		if cookie.Name == name && cookie.Value == value {
			return
		}
	}
	t.Fatalf("expected cookie %s=%s, got %v", name, value, rec.Result().Cookies())
}
