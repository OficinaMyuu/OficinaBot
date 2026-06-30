package routes

import (
	"context"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	mysqldriver "github.com/go-sql-driver/mysql"
	"github.com/labstack/echo/v4"
	"oficina-img/internal/repository"
)

func TestConfigSyncHandlerReturnsPendingConfigs(t *testing.T) {
	e := echo.New()
	store := &stubConfigSyncStore{pending: []repository.ConfigVersion{{ID: 1, Key: "bad_words"}}}
	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, "/api/service/configs/pending", nil)

	err := NewConfigSyncHandler(store).Pending(serviceContext(e, req, rec, "bot"))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusOK {
		t.Fatalf("expected status %d, got %d", http.StatusOK, rec.Code)
	}
	if !strings.Contains(rec.Body.String(), `"Key":"bad_words"`) {
		t.Fatalf("expected bad_words config, got %s", rec.Body.String())
	}
	if store.pendingClient != "bot" {
		t.Fatalf("expected bot pending lookup, got %q", store.pendingClient)
	}
}

func TestConfigSyncHandlerAcknowledgesConfig(t *testing.T) {
	e := echo.New()
	store := &stubConfigSyncStore{}
	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodPost, "/api/service/configs/1/ack", nil)
	ctx := serviceContext(e, req, rec, "bot")
	ctx.SetParamNames("version_id")
	ctx.SetParamValues("1")

	err := NewConfigSyncHandler(store).Ack(ctx)

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusNoContent {
		t.Fatalf("expected status %d, got %d", http.StatusNoContent, rec.Code)
	}
	if store.ackedVersionID != 1 || store.ackedClient != "bot" {
		t.Fatalf("expected ack bot/1, got %s/%d", store.ackedClient, store.ackedVersionID)
	}
}

func TestConfigSyncHandlerTreatsDuplicateAckAsSuccess(t *testing.T) {
	e := echo.New()
	store := &stubConfigSyncStore{ackErr: &mysqldriver.MySQLError{Number: 1062, Message: "duplicate entry"}}
	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodPost, "/api/service/configs/1/ack", nil)
	ctx := serviceContext(e, req, rec, "bot")
	ctx.SetParamNames("version_id")
	ctx.SetParamValues("1")

	err := NewConfigSyncHandler(store).Ack(ctx)

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusNoContent {
		t.Fatalf("expected status %d, got %d", http.StatusNoContent, rec.Code)
	}
}

func TestConfigSyncHandlerRejectsInvalidAckVersion(t *testing.T) {
	e := echo.New()
	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodPost, "/api/service/configs/nope/ack", nil)
	ctx := serviceContext(e, req, rec, "bot")
	ctx.SetParamNames("version_id")
	ctx.SetParamValues("nope")

	err := NewConfigSyncHandler(&stubConfigSyncStore{}).Ack(ctx)

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("expected status %d, got %d", http.StatusBadRequest, rec.Code)
	}
}

type stubConfigSyncStore struct {
	pending        []repository.ConfigVersion
	pendingClient  string
	ackedVersionID int64
	ackedClient    string
	ackErr         error
}

func (s *stubConfigSyncStore) PendingForClient(_ context.Context, clientName string) ([]repository.ConfigVersion, error) {
	s.pendingClient = clientName
	return s.pending, nil
}

func (s *stubConfigSyncStore) Acknowledge(_ context.Context, versionID int64, clientName string) error {
	s.ackedVersionID = versionID
	s.ackedClient = clientName
	return s.ackErr
}
