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

func TestIngestHandlerCreatesMessageLogBatch(t *testing.T) {
	e := echo.New()
	stores := newIngestStores()
	req := httptest.NewRequest(http.MethodPost, "/api/service/batches/message-logs", strings.NewReader(`{
		"batch_id":"batch-1",
		"logs":[{
			"guild_id":"guild",
			"channel_id":"channel",
			"message_id":"message",
			"author_id":"author",
			"content":"hello",
			"created_at":"2026-05-28T12:00:00Z"
		}]
	}`))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()
	ctx := serviceContext(e, req, rec, "bot")

	err := stores.handler.MessageLogs(ctx)

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusCreated {
		t.Fatalf("expected status %d, got %d", http.StatusCreated, rec.Code)
	}
	if len(stores.messageLogs.logs) != 1 {
		t.Fatalf("expected one message log, got %d", len(stores.messageLogs.logs))
	}
}

func TestIngestHandlerTreatsDuplicateBatchAsSuccess(t *testing.T) {
	e := echo.New()
	stores := newIngestStores()
	stores.batches.existing["batch-1"] = true
	req := httptest.NewRequest(http.MethodPost, "/api/service/batches/message-logs", strings.NewReader(`{
		"batch_id":"batch-1",
		"logs":[{"guild_id":"guild","channel_id":"channel","message_id":"message","author_id":"author","content":"hello"}]
	}`))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()

	err := stores.handler.MessageLogs(serviceContext(e, req, rec, "bot"))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusOK {
		t.Fatalf("expected status %d, got %d", http.StatusOK, rec.Code)
	}
	if len(stores.messageLogs.logs) != 0 {
		t.Fatalf("expected duplicate batch not to insert logs")
	}
}

func TestIngestHandlerCreatesRegistrationBatch(t *testing.T) {
	e := echo.New()
	stores := newIngestStores()
	req := httptest.NewRequest(http.MethodPost, "/api/service/batches/registrations", strings.NewReader(`{
		"batch_id":"batch-registrations",
		"registrations":[{
			"guild_id":"guild",
			"user_id":"user",
			"username":"Myuu",
			"registered_at":"2026-05-28T12:00:00Z",
			"metadata":{"source":"slash"}
		}]
	}`))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()

	err := stores.handler.Registrations(serviceContext(e, req, rec, "registrar"))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusCreated {
		t.Fatalf("expected status %d, got %d", http.StatusCreated, rec.Code)
	}
	if len(stores.registrations.registrations) != 1 || stores.registrations.registrations[0].MetadataJSON != `{"source":"slash"}` {
		t.Fatalf("expected registration metadata to be stored, got %+v", stores.registrations.registrations)
	}
}

func TestIngestHandlerCreatesPunishments(t *testing.T) {
	e := echo.New()
	stores := newIngestStores()
	req := httptest.NewRequest(http.MethodPost, "/api/service/batches/punishments", strings.NewReader(`{
		"batch_id":"batch-punishments",
		"punishments":[{
			"guild_id":"guild",
			"user_id":"user",
			"type":"WARN",
			"created_at":"2026-05-28T12:00:00Z"
		}]
	}`))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()

	err := stores.handler.Punishments(serviceContext(e, req, rec, "bot"))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusCreated {
		t.Fatalf("expected status %d, got %d", http.StatusCreated, rec.Code)
	}
	if len(stores.punishments.punishments) != 1 || stores.punishments.punishments[0].Type != "WARN" {
		t.Fatalf("expected WARN punishment, got %+v", stores.punishments.punishments)
	}
}

func TestIngestHandlerCreatesSyncHeartbeat(t *testing.T) {
	e := echo.New()
	stores := newIngestStores()
	req := httptest.NewRequest(http.MethodPost, "/api/service/sync-heartbeat", strings.NewReader(`{
		"status":"ok",
		"details":{"latency_ms":12},
		"checked_at":"2026-05-28T12:00:00Z"
	}`))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()

	err := stores.handler.SyncHeartbeat(serviceContext(e, req, rec, "bot"))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusCreated {
		t.Fatalf("expected status %d, got %d", http.StatusCreated, rec.Code)
	}
	if len(stores.heartbeats.heartbeats) != 1 || stores.heartbeats.heartbeats[0].DetailsJSON != `{"latency_ms":12}` {
		t.Fatalf("expected heartbeat details, got %+v", stores.heartbeats.heartbeats)
	}
}

func serviceContext(e *echo.Echo, req *http.Request, rec *httptest.ResponseRecorder, clientName string) echo.Context {
	ctx := e.NewContext(req, rec)
	ctx.Set(serviceClientContextKey, &repository.BotClient{Name: clientName})
	return ctx
}

type ingestStores struct {
	batches       *stubEventBatchStore
	messageLogs   *stubMessageLogStore
	punishments   *stubPunishmentStore
	registrations *stubRegistrationStore
	heartbeats    *stubSyncHeartbeatStore
	handler       *IngestHandler
}

func newIngestStores() *ingestStores {
	stores := &ingestStores{
		batches:       &stubEventBatchStore{existing: map[string]bool{}},
		messageLogs:   &stubMessageLogStore{},
		punishments:   &stubPunishmentStore{},
		registrations: &stubRegistrationStore{},
		heartbeats:    &stubSyncHeartbeatStore{},
	}
	stores.handler = NewIngestHandler(stores.batches, stores.messageLogs, stores.punishments, stores.registrations, stores.heartbeats)
	return stores
}

type stubEventBatchStore struct {
	existing map[string]bool
	created  []repository.EventBatch
}

func (s *stubEventBatchStore) Create(_ context.Context, batch *repository.EventBatch) error {
	s.existing[batch.ID] = true
	s.created = append(s.created, *batch)
	return nil
}

func (s *stubEventBatchStore) Exists(_ context.Context, id string) (bool, error) {
	return s.existing[id], nil
}

type stubMessageLogStore struct {
	logs []repository.MessageLog
}

func (s *stubMessageLogStore) CreateMany(_ context.Context, logs []repository.MessageLog) error {
	s.logs = append(s.logs, logs...)
	return nil
}

type stubPunishmentStore struct {
	punishments []repository.Punishment
}

func (s *stubPunishmentStore) Create(_ context.Context, punishment *repository.Punishment) error {
	s.punishments = append(s.punishments, *punishment)
	return nil
}

type stubRegistrationStore struct {
	registrations []repository.Registration
}

func (s *stubRegistrationStore) CreateMany(_ context.Context, registrations []repository.Registration) error {
	s.registrations = append(s.registrations, registrations...)
	return nil
}

type stubSyncHeartbeatStore struct {
	heartbeats []repository.SyncHeartbeat
}

func (s *stubSyncHeartbeatStore) Create(_ context.Context, heartbeat *repository.SyncHeartbeat) error {
	s.heartbeats = append(s.heartbeats, *heartbeat)
	return nil
}
