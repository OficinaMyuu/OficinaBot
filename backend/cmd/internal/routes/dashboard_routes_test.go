package routes

import (
	"context"
	"errors"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/repository"
)

func TestDashboardHandlerRequiresAdminSession(t *testing.T) {
	e := echo.New()
	handler := newTestDashboardHandler()
	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, "/api/dashboard/message-logs", nil)

	err := handler.MessageLogs(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusUnauthorized {
		t.Fatalf("expected status %d, got %d", http.StatusUnauthorized, rec.Code)
	}
}

func TestDashboardHandlerReturnsMessageLogs(t *testing.T) {
	e := echo.New()
	handler := newTestDashboardHandler()
	handler.messageLogs.(*stubDashboardMessageLogs).logs = []repository.MessageLog{{MessageID: "message"}}
	rec := httptest.NewRecorder()
	req := dashboardRequest(http.MethodGet, "/api/dashboard/message-logs?limit=10")

	err := handler.MessageLogs(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusOK {
		t.Fatalf("expected status %d, got %d", http.StatusOK, rec.Code)
	}
	if !strings.Contains(rec.Body.String(), `"MessageID":"message"`) {
		t.Fatalf("expected message log response, got %s", rec.Body.String())
	}
}

func TestDashboardHandlerRejectsInvalidLimit(t *testing.T) {
	e := echo.New()
	handler := newTestDashboardHandler()
	rec := httptest.NewRecorder()
	req := dashboardRequest(http.MethodGet, "/api/dashboard/message-logs?limit=what")

	err := handler.MessageLogs(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("expected status %d, got %d", http.StatusBadRequest, rec.Code)
	}
}

func TestDashboardHandlerReturnsOtherReadModels(t *testing.T) {
	e := echo.New()
	handler := newTestDashboardHandler()
	handler.punishments.(*stubDashboardPunishments).punishments = []repository.Punishment{{Type: "WARN"}}
	handler.registrations.(*stubDashboardRegistrations).registrations = []repository.Registration{{Username: "Myuu"}}
	handler.heartbeats.(*stubDashboardHeartbeats).heartbeats = []repository.SyncHeartbeat{{Status: "ok"}}
	handler.auditActions.(*stubDashboardAuditActions).actions = []repository.AuditAction{{Action: "config.update"}}

	tests := []struct {
		name    string
		handler func(echo.Context) error
		target  string
		want    string
	}{
		{name: "punishments", handler: handler.Punishments, target: "/api/dashboard/punishments", want: `"Type":"WARN"`},
		{name: "registrations", handler: handler.Registrations, target: "/api/dashboard/registrations", want: `"Username":"Myuu"`},
		{name: "sync health", handler: handler.SyncHealth, target: "/api/dashboard/sync-health", want: `"Status":"ok"`},
		{name: "audit actions", handler: handler.AuditActions, target: "/api/dashboard/audit-actions", want: `"Action":"config.update"`},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			rec := httptest.NewRecorder()
			err := tt.handler(e.NewContext(dashboardRequest(http.MethodGet, tt.target), rec))
			if err != nil {
				t.Fatalf("expected no echo error, got %v", err)
			}
			if rec.Code != http.StatusOK {
				t.Fatalf("expected status %d, got %d", http.StatusOK, rec.Code)
			}
			if !strings.Contains(rec.Body.String(), tt.want) {
				t.Fatalf("expected response to contain %s, got %s", tt.want, rec.Body.String())
			}
		})
	}
}

func newTestDashboardHandler() *DashboardHandler {
	return NewDashboardHandler(
		&stubDashboardAuth{user: &repository.User{DiscordID: "100", Username: "Leonardo"}},
		AuthCookieConfig{SessionName: "session"},
		&stubDashboardMessageLogs{},
		&stubDashboardPunishments{},
		&stubDashboardRegistrations{},
		&stubDashboardHeartbeats{},
		&stubDashboardAuditActions{},
	)
}

func dashboardRequest(method, target string) *http.Request {
	req := httptest.NewRequest(method, target, nil)
	req.AddCookie(&http.Cookie{Name: "session", Value: "token"})
	return req
}

type stubDashboardAuth struct {
	user *repository.User
	err  error
}

func (s *stubDashboardAuth) CurrentUser(_ context.Context, _ string) (*repository.User, error) {
	if s.err != nil {
		return nil, s.err
	}
	if s.user == nil {
		return nil, errors.New("missing user")
	}
	return s.user, nil
}

type stubDashboardMessageLogs struct {
	logs []repository.MessageLog
}

func (s *stubDashboardMessageLogs) ListRecent(_ context.Context, _ int) ([]repository.MessageLog, error) {
	return s.logs, nil
}

type stubDashboardPunishments struct {
	punishments []repository.Punishment
}

func (s *stubDashboardPunishments) ListRecent(_ context.Context, _ int) ([]repository.Punishment, error) {
	return s.punishments, nil
}

type stubDashboardRegistrations struct {
	registrations []repository.Registration
}

func (s *stubDashboardRegistrations) ListRecent(_ context.Context, _ int) ([]repository.Registration, error) {
	return s.registrations, nil
}

type stubDashboardHeartbeats struct {
	heartbeats []repository.SyncHeartbeat
}

func (s *stubDashboardHeartbeats) ListLatest(_ context.Context, _ int) ([]repository.SyncHeartbeat, error) {
	return s.heartbeats, nil
}

type stubDashboardAuditActions struct {
	actions []repository.AuditAction
}

func (s *stubDashboardAuditActions) ListRecent(_ context.Context, _ int) ([]repository.AuditAction, error) {
	return s.actions, nil
}
