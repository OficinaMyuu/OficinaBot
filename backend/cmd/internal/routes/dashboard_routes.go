package routes

import (
	"net/http"
	"os"
	"path/filepath"

	"github.com/labstack/echo/v4"
)

type DashboardRoutesConfig struct {
	AssetsPath    string
	AuthConfig    DashboardAuthConfig
	OAuthClient   DiscordOAuthClient
	Sessions      *SessionStore
	Birthdays     BirthdayRepository
	MissingConfig []string
}

func RegisterDashboardRoutes(e *echo.Echo, cfg DashboardRoutesConfig) {
	authHandler := NewDashboardAuthHandler(cfg.AuthConfig, cfg.OAuthClient, cfg.Sessions)

	e.GET("/dashboard/auth/discord/login", authHandler.Login)
	e.GET("/dashboard/auth/discord/callback", authHandler.Callback)
	e.GET("/dashboard/api/auth/me", authHandler.Me)
	e.POST("/dashboard/api/auth/logout", authHandler.RequireSession(authHandler.Logout))

	if cfg.Birthdays != nil {
		birthdayHandler := NewBirthdayHandler(cfg.Birthdays)
		birthdays := e.Group("/dashboard/api/birthdays", authHandler.RequireSession)
		birthdays.GET("", birthdayHandler.List)
		birthdays.POST("", birthdayHandler.Create)
		birthdays.PUT("/:userID", birthdayHandler.Update)
		birthdays.DELETE("/:userID", birthdayHandler.Delete)
	}

	registerDashboardAssets(e, cfg.AssetsPath)
}

func registerDashboardAssets(e *echo.Echo, assetsPath string) {
	indexPath := filepath.Join(assetsPath, "index.html")
	assetsDir := filepath.Join(assetsPath, "assets")

	e.GET("/dashboard/assets/*", echo.WrapHandler(http.StripPrefix("/dashboard/assets/", http.FileServer(http.Dir(assetsDir)))))
	e.GET("/dashboard/favicon.svg", func(c echo.Context) error {
		return serveDashboardFile(c, filepath.Join(assetsPath, "favicon.svg"), indexPath)
	})
	e.GET("/dashboard", func(c echo.Context) error {
		return serveDashboardIndex(c, indexPath)
	})
	e.GET("/dashboard/", func(c echo.Context) error {
		return serveDashboardIndex(c, indexPath)
	})
	e.GET("/dashboard/*", func(c echo.Context) error {
		return serveDashboardIndex(c, indexPath)
	})
}

func serveDashboardFile(c echo.Context, path, fallback string) error {
	if _, err := os.Stat(path); err == nil {
		return c.File(path)
	}
	return serveDashboardIndex(c, fallback)
}

func serveDashboardIndex(c echo.Context, indexPath string) error {
	if _, err := os.Stat(indexPath); err != nil {
		if os.IsNotExist(err) {
			return jsonError(c, http.StatusServiceUnavailable, "Dashboard assets are not available")
		}
		return jsonError(c, http.StatusInternalServerError, "Could not inspect dashboard assets")
	}
	return c.File(indexPath)
}
