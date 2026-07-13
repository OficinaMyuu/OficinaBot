package discord

import (
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strconv"
	"strings"
	"time"
)

const DiscordCDNBaseURL = "https://cdn.discordapp.com"
const maxLottieStickerBytes = 1024 * 1024

type StickerClient struct {
	baseURL    string
	httpClient *http.Client
}

func NewStickerClient(baseURL string) *StickerClient {
	return &StickerClient{
		baseURL:    strings.TrimRight(baseURL, "/"),
		httpClient: &http.Client{Timeout: 10 * time.Second},
	}
}

func (c *StickerClient) Lottie(ctx context.Context, stickerID int64) ([]byte, bool, error) {
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, c.baseURL+"/stickers/"+strconv.FormatInt(stickerID, 10)+".json", nil)
	if err != nil {
		return nil, false, err
	}
	req.Header.Set("Accept", "application/json")
	req.Header.Set("User-Agent", "OficinaServices Dashboard")
	response, err := c.httpClient.Do(req)
	if err != nil {
		return nil, false, err
	}
	defer response.Body.Close()
	if response.StatusCode == http.StatusNotFound {
		return nil, false, nil
	}
	if response.StatusCode < 200 || response.StatusCode >= 300 {
		return nil, false, fmt.Errorf("discord sticker request failed with status %d", response.StatusCode)
	}
	body, err := io.ReadAll(io.LimitReader(response.Body, maxLottieStickerBytes+1))
	if err != nil {
		return nil, false, err
	}
	if len(body) > maxLottieStickerBytes {
		return nil, false, fmt.Errorf("discord sticker exceeds the maximum supported size")
	}
	if !json.Valid(body) {
		return nil, false, fmt.Errorf("discord sticker response is not valid JSON")
	}
	return body, true, nil
}
