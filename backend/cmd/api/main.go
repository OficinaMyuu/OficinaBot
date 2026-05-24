package main

import (
	"github.com/joho/godotenv"
	"github.com/labstack/echo/v4"
	"github.com/playwright-community/playwright-go"
	"oficina-img/internal/routes"
	"oficina-img/internal/service"
)

func main() {
	err := godotenv.Load()
	if err != nil {
		panic(err)
	}

	err = playwright.Install()
	if err != nil {
		panic(err)
	}

	pw, err := playwright.Run()
	if err != nil {
		panic(err)
	}
	service.InitializePlaywrightService(pw)

	e := echo.New()
	e.Static("/static", "./static")

	e.POST("/api/levels/cards", routes.GetLevelCard)
	e.POST("/api/levels/roles", routes.GetLevelsRoles)

	e.GET("/api/external/videos", routes.GetVideo)

	if err := e.Start(":8080"); err != nil {
		e.Logger.Fatal(err)
	}
}
