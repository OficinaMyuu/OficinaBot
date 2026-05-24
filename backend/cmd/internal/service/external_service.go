package service

import (
	"fmt"
	"github.com/labstack/gommon/log"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"time"
)

// MaxPayloadBytes defines the maximum accepted file size for
// responses in this endpoint (8 MiB).
const MaxPayloadBytes = 8 * 1024 * 1024

func DownloadVideo(route string) (io.ReadCloser, *APIError) {
	timestamp := time.Now().UnixNano()
	filename := fmt.Sprintf("%d.mp4", timestamp)
	filePath := filepath.Join(os.TempDir(), filename)

	cmd := exec.Command("yt-dlp", "-o", filePath, "-f", "mp4", route)
	if err := cmd.Run(); err != nil {
		log.Errorf("yt-dlp failed for URL %s: %v", route, err)
		return nil, ErrorInternalServer
	}

	file, err := os.Open(filePath)
	if err != nil {
		log.Error(err)
		return nil, ErrorInternalServer
	}

	info, err := file.Stat()
	if err != nil {
		log.Errorf("failed to stat fil: %v", err)
		file.Close()
		return nil, ErrorInternalServer
	}

	fileSize := info.Size()
	if fileSize > MaxPayloadBytes {
		file.Close()
		os.Remove(filePath)
		return nil, ErrorResponseTooLarge(fileSize, MaxPayloadBytes)
	}

	go func(path string) {
		time.Sleep(1 * time.Minute)
		os.Remove(path)
	}(filePath)

	return file, nil
}
