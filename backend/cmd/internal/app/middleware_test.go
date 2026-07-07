package app

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/labstack/echo/v4"
)

func TestRegisterMiddlewareAllowsAnyOriginWithCredentials(t *testing.T) {
	e := echo.New()
	registerMiddleware(e, Config{BodyLimit: DefaultBodyLimit})
	e.GET("/health", func(c echo.Context) error {
		return c.NoContent(http.StatusNoContent)
	})

	origin := "https://oficinamyuu.com.br"
	requestHeaders := "authorization,x-csrf-token,content-type"
	req := httptest.NewRequest(http.MethodOptions, "/health", nil)
	req.Header.Set(echo.HeaderOrigin, origin)
	req.Header.Set(echo.HeaderAccessControlRequestMethod, http.MethodGet)
	req.Header.Set(echo.HeaderAccessControlRequestHeaders, requestHeaders)
	rec := httptest.NewRecorder()

	e.ServeHTTP(rec, req)

	if got := rec.Header().Get(echo.HeaderAccessControlAllowOrigin); got != origin {
		t.Fatalf("expected reflected CORS origin, got %q", got)
	}
	if got := rec.Header().Get(echo.HeaderAccessControlAllowCredentials); got != "true" {
		t.Fatalf("expected credentials to be enabled, got %q", got)
	}
	if got := rec.Header().Get(echo.HeaderAccessControlAllowHeaders); got != requestHeaders {
		t.Fatalf("expected requested headers to be allowed, got %q", got)
	}
}
