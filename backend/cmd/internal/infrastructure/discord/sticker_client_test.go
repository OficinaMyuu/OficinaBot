package discord

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestStickerClientLoadsAndValidatesLottieJSON(t *testing.T) {
	server := httptest.NewServer(http.HandlerFunc(func(response http.ResponseWriter, request *http.Request) {
		if request.URL.Path != "/stickers/749054660769218631.json" {
			t.Fatalf("unexpected sticker path %s", request.URL.Path)
		}
		response.Header().Set("Content-Type", "application/json")
		_, _ = response.Write([]byte(`{"v":"5.6.2","layers":[]}`))
	}))
	defer server.Close()

	body, found, err := NewStickerClient(server.URL).Lottie(context.Background(), 749054660769218631)
	if err != nil {
		t.Fatalf("load lottie sticker: %v", err)
	}
	if !found || len(body) == 0 {
		t.Fatalf("expected lottie sticker body, found=%t body=%q", found, body)
	}
}

func TestStickerClientMapsMissingAsset(t *testing.T) {
	server := httptest.NewServer(http.NotFoundHandler())
	defer server.Close()

	_, found, err := NewStickerClient(server.URL).Lottie(context.Background(), 1)
	if err != nil || found {
		t.Fatalf("expected missing sticker, found=%t err=%v", found, err)
	}
}
