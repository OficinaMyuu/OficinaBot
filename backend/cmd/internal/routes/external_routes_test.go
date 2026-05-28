package routes

import (
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/service"
)

type stubVideoDownloader struct {
	body        string
	err         *service.APIError
	calledWith  string
	calledCount int
}

func (s *stubVideoDownloader) DownloadVideo(route string) (io.ReadCloser, *service.APIError) {
	s.calledWith = route
	s.calledCount++
	return io.NopCloser(strings.NewReader(s.body)), s.err
}

func TestIsSupportedVideoURL(t *testing.T) {
	tests := []struct {
		name string
		url  string
		want bool
	}{
		{name: "root Twitter domain", url: "https://twitter.com/oficina", want: true},
		{name: "Twitter subdomain", url: "https://mobile.twitter.com/oficina", want: true},
		{name: "X domain", url: "https://x.com/oficina", want: true},
		{name: "Instagram domain", url: "https://instagram.com/oficina", want: true},
		{name: "lookalike domain", url: "https://notinstagram.com/oficina", want: false},
		{name: "missing host", url: "/oficina", want: false},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			if got := IsSupportedVideoURL(tt.url); got != tt.want {
				t.Fatalf("expected %v, got %v", tt.want, got)
			}
		})
	}
}

func TestExternalHandlerRequiresURL(t *testing.T) {
	e := echo.New()
	downloader := &stubVideoDownloader{}
	req := httptest.NewRequest(http.MethodGet, "/api/external/videos", nil)
	rec := httptest.NewRecorder()

	err := NewExternalHandler(downloader).GetVideo(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("expected status %d, got %d", http.StatusBadRequest, rec.Code)
	}
	if downloader.calledCount != 0 {
		t.Fatal("expected downloader not to be called")
	}
	assertAPIError(t, rec.Body.String(), service.ErrorURLNotPresent)
}

func TestExternalHandlerRejectsUnsupportedURL(t *testing.T) {
	e := echo.New()
	downloader := &stubVideoDownloader{}
	req := httptest.NewRequest(http.MethodGet, "/api/external/videos?url=https://example.com/video", nil)
	rec := httptest.NewRecorder()

	err := NewExternalHandler(downloader).GetVideo(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("expected status %d, got %d", http.StatusBadRequest, rec.Code)
	}
	if downloader.calledCount != 0 {
		t.Fatal("expected downloader not to be called")
	}
}

func TestExternalHandlerStreamsDownloadedVideo(t *testing.T) {
	e := echo.New()
	downloader := &stubVideoDownloader{body: "video"}
	videoURL := "https://twitter.com/oficina/status/1"
	req := httptest.NewRequest(http.MethodGet, "/api/external/videos?url="+videoURL, nil)
	rec := httptest.NewRecorder()

	err := NewExternalHandler(downloader).GetVideo(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusOK {
		t.Fatalf("expected status %d, got %d", http.StatusOK, rec.Code)
	}
	if rec.Body.String() != "video" {
		t.Fatalf("expected streamed body %q, got %q", "video", rec.Body.String())
	}
	if downloader.calledWith != videoURL {
		t.Fatalf("expected downloader URL %q, got %q", videoURL, downloader.calledWith)
	}
}

func TestExternalHandlerReturnsDownloaderError(t *testing.T) {
	e := echo.New()
	expected := service.NewError(http.StatusBadGateway, "download failed")
	downloader := &stubVideoDownloader{err: expected}
	req := httptest.NewRequest(http.MethodGet, "/api/external/videos?url=https://x.com/oficina/status/1", nil)
	rec := httptest.NewRecorder()

	err := NewExternalHandler(downloader).GetVideo(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusBadGateway {
		t.Fatalf("expected status %d, got %d", http.StatusBadGateway, rec.Code)
	}
	assertAPIError(t, rec.Body.String(), expected)
}
