package handler

import (
	"github.com/labstack/echo/v4"
)

type DashboardRoutesConfig struct {
	AuthConfig    DashboardAuthConfig
	OAuthClient   DiscordOAuthClient
	Sessions      *SessionStore
	Birthdays     BirthdayRepository
	Tickets       TicketRepository
	Messages      MessageRepository
	StoreItems    StoreItemSettingsRepository
	Users         UserRepository
	Directory     *GuildDirectoryHandler
	Stickers      LottieStickerClient
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
	}
	if cfg.Messages != nil {
		messageHandler := NewChannelMessageHandler(cfg.Messages)
		channels := e.Group("/channels", authHandler.RequireSession)
		channels.GET("/:channelID/messages", messageHandler.List)
		channels.GET("/:channelID/messages/:messageID/versions", messageHandler.Versions)
	}

	if cfg.StoreItems != nil {
		storeItemSettingsHandler := NewStoreItemSettingsHandler(cfg.StoreItems)
		storeItemSettings := e.Group("/economy/action-costs", authHandler.RequireSession)
		storeItemSettings.GET("", storeItemSettingsHandler.List)
		storeItemSettings.PATCH("/:itemType", storeItemSettingsHandler.Update)
	}

	if cfg.Users != nil {
		userHandler := NewUserHandler(cfg.Users)
		users := e.Group("/users", authHandler.RequireSession)
		users.POST("/query", userHandler.Query)
	}
	if cfg.Directory != nil {
		directory := e.Group("/discord", authHandler.RequireSession)
		directory.GET("/guild-directory", cfg.Directory.Get)
	}
	if cfg.Stickers != nil {
		stickers := e.Group("/discord/stickers", authHandler.RequireSession)
		stickers.GET("/:stickerID/lottie", NewStickerHandler(cfg.Stickers).Lottie)
	}
}
