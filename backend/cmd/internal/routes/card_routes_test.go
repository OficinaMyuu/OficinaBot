package routes

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/service"
)

type stubCardRenderer struct {
	levelCard    []byte
	levelCardErr *service.APIError
	roles        []byte
	rolesErr     *service.APIError
}

func (s *stubCardRenderer) GenerateLevelCard(_ *service.LevelDataDTO) ([]byte, *service.APIError) {
	return s.levelCard, s.levelCardErr
}

func (s *stubCardRenderer) GenerateLevelsRoles(_ *service.LevelsRolesData) ([]byte, *service.APIError) {
	return s.roles, s.rolesErr
}

func TestCardHandlerReturnsMalformedJSON(t *testing.T) {
	e := echo.New()
	req := httptest.NewRequest(http.MethodPost, "/api/levels/cards", strings.NewReader("{"))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()

	err := NewCardHandler(&stubCardRenderer{}).GetLevelCard(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("expected status %d, got %d", http.StatusBadRequest, rec.Code)
	}
	assertAPIError(t, rec.Body.String(), service.ErrorMalformedJSON)
}

func TestCardHandlerEncodesRendererResponse(t *testing.T) {
	e := echo.New()
	req := httptest.NewRequest(http.MethodPost, "/api/levels/cards", strings.NewReader(`{"username":"Myuu"}`))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()

	err := NewCardHandler(&stubCardRenderer{levelCard: []byte{1, 2, 3}}).GetLevelCard(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusOK {
		t.Fatalf("expected status %d, got %d", http.StatusOK, rec.Code)
	}

	var body map[string]string
	if err := json.Unmarshal(rec.Body.Bytes(), &body); err != nil {
		t.Fatalf("expected JSON response, got %v", err)
	}
	if body["image"] != "AQID" {
		t.Fatalf("expected base64 image AQID, got %q", body["image"])
	}
}

func TestCardHandlerEncodesRolesRendererResponse(t *testing.T) {
	e := echo.New()
	req := httptest.NewRequest(http.MethodPost, "/api/levels/roles", strings.NewReader(`{"guild":{"name":"Oficina","icon_url":"https://example.com/icon.png"},"levels":[]}`))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()

	err := NewCardHandler(&stubCardRenderer{roles: []byte{4, 5, 6}}).GetLevelsRoles(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusOK {
		t.Fatalf("expected status %d, got %d", http.StatusOK, rec.Code)
	}

	var body map[string]string
	if err := json.Unmarshal(rec.Body.Bytes(), &body); err != nil {
		t.Fatalf("expected JSON response, got %v", err)
	}
	if body["image"] != "BAUG" {
		t.Fatalf("expected base64 image BAUG, got %q", body["image"])
	}
}

func TestCardHandlerReturnsRendererError(t *testing.T) {
	e := echo.New()
	expected := service.NewError(http.StatusTeapot, "renderer exploded")
	req := httptest.NewRequest(http.MethodPost, "/api/levels/cards", strings.NewReader(`{"username":"Myuu"}`))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()

	err := NewCardHandler(&stubCardRenderer{levelCardErr: expected}).GetLevelCard(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusTeapot {
		t.Fatalf("expected status %d, got %d", http.StatusTeapot, rec.Code)
	}
	assertAPIError(t, rec.Body.String(), expected)
}

func TestCardHandlerReturnsRolesRendererError(t *testing.T) {
	e := echo.New()
	expected := service.NewError(http.StatusTeapot, "roles renderer exploded")
	req := httptest.NewRequest(http.MethodPost, "/api/levels/roles", strings.NewReader(`{"levels":[]}`))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()

	err := NewCardHandler(&stubCardRenderer{rolesErr: expected}).GetLevelsRoles(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusTeapot {
		t.Fatalf("expected status %d, got %d", http.StatusTeapot, rec.Code)
	}
	assertAPIError(t, rec.Body.String(), expected)
}

func assertAPIError(t *testing.T, raw string, expected *service.APIError) {
	t.Helper()

	var got service.APIError
	if err := json.Unmarshal([]byte(raw), &got); err != nil {
		t.Fatalf("expected API error JSON, got %v", err)
	}
	if got.Status != expected.Status || got.Message != expected.Message {
		t.Fatalf("expected error %+v, got %+v", expected, got)
	}
}
