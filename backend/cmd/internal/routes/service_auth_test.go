package routes

import (
	"context"
	"errors"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/auth"
	"oficina-img/internal/repository"
)

func TestServiceAuthMiddlewareSetsServiceClient(t *testing.T) {
	e := echo.New()
	authenticator := &stubServiceAuthenticator{client: &repository.BotClient{Name: "bot"}}
	req := httptest.NewRequest(http.MethodGet, "/api/service/me", nil)
	req.Header.Set(echo.HeaderAuthorization, "Bearer token")
	rec := httptest.NewRecorder()
	ctx := e.NewContext(req, rec)

	handler := ServiceAuthMiddleware(authenticator)(func(c echo.Context) error {
		client, ok := ServiceClient(c)
		if !ok {
			t.Fatal("expected service client on context")
		}
		return c.String(http.StatusOK, client.Name)
	})

	if err := handler(ctx); err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusOK || rec.Body.String() != "bot" {
		t.Fatalf("expected bot response, got %d %q", rec.Code, rec.Body.String())
	}
}

func TestServiceAuthMiddlewareRejectsInvalidToken(t *testing.T) {
	e := echo.New()
	authenticator := &stubServiceAuthenticator{err: auth.ErrInvalidServiceToken}
	req := httptest.NewRequest(http.MethodGet, "/api/service/me", nil)
	rec := httptest.NewRecorder()

	handler := ServiceAuthMiddleware(authenticator)(func(c echo.Context) error {
		return c.NoContent(http.StatusOK)
	})

	if err := handler(e.NewContext(req, rec)); err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusUnauthorized {
		t.Fatalf("expected status %d, got %d", http.StatusUnauthorized, rec.Code)
	}
}

func TestServiceAuthMiddlewarePropagatesUnexpectedErrors(t *testing.T) {
	e := echo.New()
	expected := errors.New("database fell over")
	authenticator := &stubServiceAuthenticator{err: expected}
	req := httptest.NewRequest(http.MethodGet, "/api/service/me", nil)
	rec := httptest.NewRecorder()

	handler := ServiceAuthMiddleware(authenticator)(func(c echo.Context) error {
		return c.NoContent(http.StatusOK)
	})

	if err := handler(e.NewContext(req, rec)); !errors.Is(err, expected) {
		t.Fatalf("expected propagated error, got %v", err)
	}
}

type stubServiceAuthenticator struct {
	client *repository.BotClient
	err    error
}

func (s *stubServiceAuthenticator) Authenticate(_ context.Context, _ string) (*repository.BotClient, error) {
	if s.err != nil {
		return nil, s.err
	}
	return s.client, nil
}
