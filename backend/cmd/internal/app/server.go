package app

import (
	"github.com/labstack/echo/v4"
	"github.com/playwright-community/playwright-go"
	"oficina-img/internal/routes"
	"oficina-img/internal/service"
)

func NewServer() (*echo.Echo, error) {
	if err := playwright.Install(); err != nil {
		return nil, err
	}

	pw, err := playwright.Run()
	if err != nil {
		return nil, err
	}
	service.InitializePlaywrightService(pw)

	e := echo.New()
	registerRoutes(e)
	return e, nil
}

func registerRoutes(e *echo.Echo) {
	e.Static("/static", "./static")

	e.POST("/api/levels/cards", routes.GetLevelCard)
	e.POST("/api/levels/roles", routes.GetLevelsRoles)
	e.GET("/api/external/videos", routes.GetVideo)
}
