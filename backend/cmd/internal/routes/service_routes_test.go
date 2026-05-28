package routes

import (
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/repository"
)

func TestServiceHandlerMeReturnsServiceClient(t *testing.T) {
	e := echo.New()
	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, "/api/service/me", nil)
	ctx := e.NewContext(req, rec)
	ctx.Set(serviceClientContextKey, &repository.BotClient{Name: "registrar"})

	err := NewServiceHandler().Me(ctx)

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusOK {
		t.Fatalf("expected status %d, got %d", http.StatusOK, rec.Code)
	}
	if !strings.Contains(rec.Body.String(), `"name":"registrar"`) {
		t.Fatalf("expected registrar response, got %s", rec.Body.String())
	}
}

func TestServiceHandlerMeRejectsMissingClient(t *testing.T) {
	e := echo.New()
	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, "/api/service/me", nil)

	err := NewServiceHandler().Me(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusUnauthorized {
		t.Fatalf("expected status %d, got %d", http.StatusUnauthorized, rec.Code)
	}
}
