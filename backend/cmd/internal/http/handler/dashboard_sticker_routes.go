package handler

import (
	"context"
	"net/http"

	"github.com/labstack/echo/v4"
)

type LottieStickerClient interface {
	Lottie(ctx context.Context, stickerID int64) ([]byte, bool, error)
}

type StickerHandler struct {
	client LottieStickerClient
}

func NewStickerHandler(client LottieStickerClient) *StickerHandler {
	return &StickerHandler{client: client}
}

func (h *StickerHandler) Lottie(c echo.Context) error {
	stickerID, err := parseSnowflake(c.Param("stickerID"), "sticker")
	if err != nil {
		return jsonError(c, http.StatusBadRequest, err.Error())
	}
	body, found, err := h.client.Lottie(c.Request().Context(), stickerID)
	if err != nil {
		return jsonError(c, http.StatusBadGateway, "Could not load Discord sticker")
	}
	if !found {
		return jsonError(c, http.StatusNotFound, "Lottie sticker not found")
	}
	c.Response().Header().Set("Cache-Control", "public, max-age=31536000, immutable")
	return c.Blob(http.StatusOK, "application/json", body)
}
