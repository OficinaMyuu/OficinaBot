package handler

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/labstack/echo/v4"
)

type fakeLottieStickerClient struct {
	found bool
}

func (f fakeLottieStickerClient) Lottie(_ context.Context, _ int64) ([]byte, bool, error) {
	return []byte(`{"v":"5.6.2"}`), f.found, nil
}

func TestStickerHandlerReturnsCacheableLottieJSON(t *testing.T) {
	e := echo.New()
	req := httptest.NewRequest(http.MethodGet, "/discord/stickers/1/lottie", nil)
	rec := httptest.NewRecorder()
	ctx := e.NewContext(req, rec)
	ctx.SetParamNames("stickerID")
	ctx.SetParamValues("1")

	if err := NewStickerHandler(fakeLottieStickerClient{found: true}).Lottie(ctx); err != nil {
		t.Fatalf("lottie returned error: %v", err)
	}
	if rec.Code != http.StatusOK || rec.Header().Get("Content-Type") != "application/json" {
		t.Fatalf("expected JSON response, status=%d content-type=%s", rec.Code, rec.Header().Get("Content-Type"))
	}
	if rec.Header().Get("Cache-Control") == "" {
		t.Fatal("expected immutable cache policy")
	}
}
