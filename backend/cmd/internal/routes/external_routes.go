package routes

import (
	"github.com/labstack/echo/v4"
	"io"
	"net/http"
	"net/url"
	"oficina-img/internal/service"
	"strings"
)

// I created a war inside of me whether I added "x.com" or not,
// cause its name is not X, its Twitter.
var Supported = []string{"instagram.com", "twitter.com", "x.com"}

type VideoDownloader interface {
	DownloadVideo(route string) (io.ReadCloser, *service.APIError)
}

type ExternalHandler struct {
	downloader VideoDownloader
}

func NewExternalHandler(downloader VideoDownloader) *ExternalHandler {
	return &ExternalHandler{downloader: downloader}
}

func (h *ExternalHandler) GetVideo(c echo.Context) error {
	endpoint := c.QueryParam("url")
	if endpoint == "" {
		return c.JSON(400, service.ErrorURLNotPresent)
	}

	if !IsSupportedVideoURL(endpoint) {
		return c.JSON(400, service.ErrorDomainNotSupported(endpoint))
	}

	file, err := h.downloader.DownloadVideo(endpoint)
	if err != nil {
		return c.JSON(err.Status, err)
	}
	defer file.Close()

	return c.Stream(http.StatusOK, "video/mp4", file)
}

func IsSupportedVideoURL(route string) bool {
	parsed, err := url.Parse(route)
	if err != nil || parsed.Host == "" {
		return false
	}

	host := strings.ToLower(parsed.Host)
	for _, domain := range Supported {
		if host == domain || strings.HasSuffix(host, "."+domain) {
			return true
		}
	}
	return false
}
