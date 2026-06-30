package app

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/labstack/echo/v4"
)

func TestRegisterMiddlewareAllowsAnyOriginWithoutCredentials(t *testing.T) {
	e := echo.New()
	registerMiddleware(e, Config{BodyLimit: DefaultBodyLimit})
	e.GET("/health", func(c echo.Context) error {
		return c.NoContent(http.StatusNoContent)
	})

	req := httptest.NewRequest(http.MethodOptions, "/health", nil)
	req.Header.Set(echo.HeaderOrigin, "https://example.invalid")
	req.Header.Set(echo.HeaderAccessControlRequestMethod, http.MethodGet)
	rec := httptest.NewRecorder()

	e.ServeHTTP(rec, req)

	if got := rec.Header().Get(echo.HeaderAccessControlAllowOrigin); got != "*" {
		t.Fatalf("expected wildcard CORS origin, got %q", got)
	}
	if got := rec.Header().Get(echo.HeaderAccessControlAllowCredentials); got != "" {
		t.Fatalf("expected credentials to be disabled, got %q", got)
	}
}
