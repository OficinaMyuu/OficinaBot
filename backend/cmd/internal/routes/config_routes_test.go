package routes

import (
	"context"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/repository"
)

func TestConfigHandlerCreatesConfigVersionAndAudit(t *testing.T) {
	e := echo.New()
	configs := &stubConfigStore{}
	audit := &stubConfigAuditStore{}
	handler := NewConfigHandler(
		&stubDashboardAuth{user: &repository.User{DiscordID: "100", Username: "Leonardo"}},
		AuthCookieConfig{SessionName: "session"},
		configs,
		audit,
	)
	req := dashboardRequestWithBody(http.MethodPost, "/api/dashboard/configs", `{"scope":"automod","key":"bad_words","value":["bad"]}`)
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()

	err := handler.CreateConfig(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusCreated {
		t.Fatalf("expected status %d, got %d", http.StatusCreated, rec.Code)
	}
	if len(configs.created) != 1 || configs.created[0].ValueJSON != `["bad"]` {
		t.Fatalf("expected config value to be stored, got %+v", configs.created)
	}
	if len(audit.created) != 1 || audit.created[0].Action != "config.create" {
		t.Fatalf("expected audit action, got %+v", audit.created)
	}
}

func TestConfigHandlerRejectsInvalidJSONValue(t *testing.T) {
	e := echo.New()
	handler := NewConfigHandler(
		&stubDashboardAuth{user: &repository.User{DiscordID: "100", Username: "Leonardo"}},
		AuthCookieConfig{SessionName: "session"},
		&stubConfigStore{},
		&stubConfigAuditStore{},
	)
	req := dashboardRequestWithBody(http.MethodPost, "/api/dashboard/configs", `{"scope":"automod","key":"bad_words","value":}`)
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()

	err := handler.CreateConfig(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("expected status %d, got %d", http.StatusBadRequest, rec.Code)
	}
}

func TestConfigHandlerListsConfigs(t *testing.T) {
	e := echo.New()
	configs := &stubConfigStore{recent: []repository.ConfigVersion{{Scope: "automod", Key: "bad_words"}}}
	handler := NewConfigHandler(
		&stubDashboardAuth{user: &repository.User{DiscordID: "100", Username: "Leonardo"}},
		AuthCookieConfig{SessionName: "session"},
		configs,
		&stubConfigAuditStore{},
	)
	rec := httptest.NewRecorder()

	err := handler.ListConfigs(e.NewContext(dashboardRequest(http.MethodGet, "/api/dashboard/configs"), rec))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusOK {
		t.Fatalf("expected status %d, got %d", http.StatusOK, rec.Code)
	}
	if !strings.Contains(rec.Body.String(), `"Key":"bad_words"`) {
		t.Fatalf("expected config response, got %s", rec.Body.String())
	}
}

func dashboardRequestWithBody(method, target, body string) *http.Request {
	req := httptest.NewRequest(method, target, strings.NewReader(body))
	req.AddCookie(&http.Cookie{Name: "session", Value: "token"})
	return req
}

type stubConfigStore struct {
	created []repository.ConfigVersion
	recent  []repository.ConfigVersion
}

func (s *stubConfigStore) Create(_ context.Context, version *repository.ConfigVersion) error {
	version.ID = int64(len(s.created) + 1)
	s.created = append(s.created, *version)
	return nil
}

func (s *stubConfigStore) ListRecent(_ context.Context, _ int) ([]repository.ConfigVersion, error) {
	return s.recent, nil
}

type stubConfigAuditStore struct {
	created []repository.AuditAction
}

func (s *stubConfigAuditStore) Create(_ context.Context, action *repository.AuditAction) error {
	s.created = append(s.created, *action)
	return nil
}
