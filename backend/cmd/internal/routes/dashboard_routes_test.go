package routes

import (
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
	"strings"
	"testing"

	"github.com/labstack/echo/v4"
)

func TestDashboardAssetsServeBuiltFiles(t *testing.T) {
	assetsPath := t.TempDir()
	assetsDir := filepath.Join(assetsPath, "assets")
	if err := os.Mkdir(assetsDir, 0755); err != nil {
		t.Fatalf("create dashboard assets dir: %v", err)
	}

	indexHTML := "<!doctype html><html><body>dashboard</body></html>"
	if err := os.WriteFile(filepath.Join(assetsPath, "index.html"), []byte(indexHTML), 0644); err != nil {
		t.Fatalf("write dashboard index: %v", err)
	}

	css := "body { color: #111; }\n"
	if err := os.WriteFile(filepath.Join(assetsDir, "index-test.css"), []byte(css), 0644); err != nil {
		t.Fatalf("write dashboard css: %v", err)
	}

	e := echo.New()
	RegisterDashboardRoutes(e, DashboardRoutesConfig{
		AssetsPath: assetsPath,
		Sessions:   NewSessionStore(),
	})

	req := httptest.NewRequest(http.MethodGet, "/dashboard/assets/index-test.css", nil)
	rec := httptest.NewRecorder()

	e.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("expected status %d, got %d with body %q", http.StatusOK, rec.Code, rec.Body.String())
	}
	if got := rec.Body.String(); got != css {
		t.Fatalf("expected CSS asset body %q, got %q", css, got)
	}
	if strings.Contains(strings.ToLower(rec.Body.String()), "<html") {
		t.Fatalf("expected asset response, got HTML fallback %q", rec.Body.String())
	}
	if got := rec.Header().Get(echo.HeaderContentType); !strings.HasPrefix(got, "text/css") {
		t.Fatalf("expected CSS content type, got %q", got)
	}
}
