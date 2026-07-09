package routes

import (
	"github.com/labstack/echo/v4"
)

type DashboardRoutesConfig struct {
	AuthConfig    DashboardAuthConfig
	OAuthClient   DiscordOAuthClient
	Sessions      *SessionStore
	Birthdays     BirthdayRepository
	Tickets       TicketRepository
	MissingConfig []string
}

func RegisterDashboardRoutes(e *echo.Echo, cfg DashboardRoutesConfig) {
	authHandler := NewDashboardAuthHandler(cfg.AuthConfig, cfg.OAuthClient, cfg.Sessions)

	e.GET("/auth/discord/login", authHandler.Login)
	e.GET("/auth/discord/callback", authHandler.Callback)
	e.GET("/auth/me", authHandler.Me)
	e.POST("/auth/logout", authHandler.RequireSession(authHandler.Logout))

	if cfg.Birthdays != nil {
		birthdayHandler := NewBirthdayHandler(cfg.Birthdays)
		birthdays := e.Group("/birthdays", authHandler.RequireSession)
		birthdays.GET("", birthdayHandler.List)
		birthdays.POST("", birthdayHandler.Create)
		birthdays.PUT("/:userID", birthdayHandler.Update)
		birthdays.DELETE("/:userID", birthdayHandler.Delete)
	}

	if cfg.Tickets != nil {
		ticketHandler := NewTicketHandler(cfg.Tickets)
		tickets := e.Group("/tickets", authHandler.RequireSession)
		tickets.GET("", ticketHandler.List)
		tickets.GET("/:ticketID/messages", ticketHandler.Messages)
	}
}
