package routes

import (
	"github.com/labstack/echo/v4"
	"net/http"
	"oficina-img/internal/service"
)

type CardRenderer interface {
	GenerateLevelCard(ld *service.LevelDataDTO) ([]byte, *service.APIError)
	GenerateLevelsRoles(lrd *service.LevelsRolesData) ([]byte, *service.APIError)
}

type CardHandler struct {
	renderer CardRenderer
}

const cardImageContentType = "image/png"

func NewCardHandler(renderer CardRenderer) *CardHandler {
	return &CardHandler{renderer: renderer}
}

func (h *CardHandler) GetLevelCard(c echo.Context) error {
	var ld service.LevelDataDTO
	if err := c.Bind(&ld); err != nil {
		return c.JSON(http.StatusBadRequest, service.ErrorMalformedJSON)
	}

	img, err := h.renderer.GenerateLevelCard(&ld)
	if err != nil {
		return c.JSON(err.Status, err)
	}

	return c.Blob(http.StatusOK, cardImageContentType, img)
}

func (h *CardHandler) GetLevelsRoles(c echo.Context) error {
	var lrd service.LevelsRolesData
	if err := c.Bind(&lrd); err != nil {
		return c.JSON(http.StatusBadRequest, service.ErrorMalformedJSON)
	}

	img, err := h.renderer.GenerateLevelsRoles(&lrd)
	if err != nil {
		return c.JSON(err.Status, err)
	}

	return c.Blob(http.StatusOK, cardImageContentType, img)
}
