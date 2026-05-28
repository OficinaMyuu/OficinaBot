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

func TestAdminHandlerListsUsersForOwner(t *testing.T) {
	e := echo.New()
	authService := newStubAdminManagementService()
	authService.users = []repository.User{{DiscordID: "owner", Username: "Leonardo"}}
	rec := httptest.NewRecorder()
	req := adminRequest(http.MethodGet, "/api/admin/users", "")

	err := newTestAdminHandler(authService).ListUsers(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusOK {
		t.Fatalf("expected status %d, got %d", http.StatusOK, rec.Code)
	}
	if !strings.Contains(rec.Body.String(), `"discord_id":"owner"`) {
		t.Fatalf("expected owner in response, got %s", rec.Body.String())
	}
}

func TestAdminHandlerRejectsListForNonOwner(t *testing.T) {
	e := echo.New()
	authService := newStubAdminManagementService()
	authService.listErr = auth.ErrOwnerOnlyOperation
	rec := httptest.NewRecorder()
	req := adminRequest(http.MethodGet, "/api/admin/users", "")

	err := newTestAdminHandler(authService).ListUsers(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusForbidden {
		t.Fatalf("expected status %d, got %d", http.StatusForbidden, rec.Code)
	}
}

func TestAdminHandlerAddsUser(t *testing.T) {
	e := echo.New()
	authService := newStubAdminManagementService()
	rec := httptest.NewRecorder()
	req := adminRequest(http.MethodPost, "/api/admin/users", `{"discord_id":"200","username":"Admin"}`)
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)

	err := newTestAdminHandler(authService).AddUser(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusCreated {
		t.Fatalf("expected status %d, got %d", http.StatusCreated, rec.Code)
	}
	if authService.addedDiscordID != "200" {
		t.Fatalf("expected added discord id 200, got %q", authService.addedDiscordID)
	}
}

func TestAdminHandlerRejectsInvalidAddRequest(t *testing.T) {
	e := echo.New()
	rec := httptest.NewRecorder()
	req := adminRequest(http.MethodPost, "/api/admin/users", `{"discord_id":""}`)
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)

	err := newTestAdminHandler(newStubAdminManagementService()).AddUser(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("expected status %d, got %d", http.StatusBadRequest, rec.Code)
	}
}

func TestAdminHandlerRemovesUser(t *testing.T) {
	e := echo.New()
	authService := newStubAdminManagementService()
	rec := httptest.NewRecorder()
	req := adminRequest(http.MethodDelete, "/api/admin/users/200", "")
	ctx := e.NewContext(req, rec)
	ctx.SetParamNames("discord_id")
	ctx.SetParamValues("200")

	err := newTestAdminHandler(authService).RemoveUser(ctx)

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusNoContent {
		t.Fatalf("expected status %d, got %d", http.StatusNoContent, rec.Code)
	}
	if authService.removedDiscordID != "200" {
		t.Fatalf("expected removed discord id 200, got %q", authService.removedDiscordID)
	}
}

func TestAdminHandlerPreventsOwnerRemoval(t *testing.T) {
	e := echo.New()
	authService := newStubAdminManagementService()
	authService.removeErr = auth.ErrOwnerRemoval
	rec := httptest.NewRecorder()
	req := adminRequest(http.MethodDelete, "/api/admin/users/owner", "")
	ctx := e.NewContext(req, rec)
	ctx.SetParamNames("discord_id")
	ctx.SetParamValues("owner")

	err := newTestAdminHandler(authService).RemoveUser(ctx)

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("expected status %d, got %d", http.StatusBadRequest, rec.Code)
	}
}

func newTestAdminHandler(authService *stubAdminManagementService) *AdminHandler {
	return NewAdminHandler(authService, AuthCookieConfig{SessionName: "session"})
}

func adminRequest(method, target, body string) *http.Request {
	req := httptest.NewRequest(method, target, strings.NewReader(body))
	req.AddCookie(&http.Cookie{Name: "session", Value: "token"})
	return req
}

type stubAdminManagementService struct {
	actor            *repository.User
	users            []repository.User
	listErr          error
	addedDiscordID   string
	removedDiscordID string
	removeErr        error
}

func newStubAdminManagementService() *stubAdminManagementService {
	return &stubAdminManagementService{actor: &repository.User{DiscordID: "owner", Username: "Leonardo"}}
}

func (s *stubAdminManagementService) CurrentUser(_ context.Context, _ string) (*repository.User, error) {
	if s.actor == nil {
		return nil, errors.New("missing actor")
	}
	return s.actor, nil
}

func (s *stubAdminManagementService) ListAdmins(_ context.Context, _ *repository.User) ([]repository.User, error) {
	return s.users, s.listErr
}

func (s *stubAdminManagementService) AddAdmin(_ context.Context, _ *repository.User, discordID, username string) (*repository.User, error) {
	s.addedDiscordID = discordID
	return &repository.User{DiscordID: discordID, Username: username}, nil
}

func (s *stubAdminManagementService) RemoveAdmin(_ context.Context, _ *repository.User, discordID string) error {
	s.removedDiscordID = discordID
	return s.removeErr
}

func (s *stubAdminManagementService) IsOwner(user *repository.User) bool {
	return user != nil && user.DiscordID == "owner"
}
