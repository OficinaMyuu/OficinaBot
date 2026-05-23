package routes

import (
	"encoding/base64"
	"github.com/labstack/echo/v4"
	"net/http"
	"oficina-img/internal/service"
)

func GetLevelCard(c echo.Context) error {
	var ld service.LevelDataDTO
	if err := c.Bind(&ld); err != nil {
		return c.JSON(http.StatusBadRequest, service.ErrorMalformedJSON)
	}

	img, err := service.GenerateLevelCard(&ld)
	if err != nil {
		return c.JSON(err.Status, err)
	}

	resp := map[string]string{
		"image": base64.StdEncoding.EncodeToString(img),
	}
	return c.JSON(http.StatusOK, &resp)
}

func GetLevelsRoles(c echo.Context) error {
	var lrd service.LevelsRolesData
	if err := c.Bind(&lrd); err != nil {
		return c.JSON(http.StatusBadRequest, service.ErrorMalformedJSON)
	}

	img, err := service.GenerateLevelsRoles(&lrd)
	if err != nil {
		return c.JSON(err.Status, err)
	}

	resp := map[string]string{
		"image": base64.StdEncoding.EncodeToString(img),
	}
	return c.JSON(http.StatusOK, &resp)
}
