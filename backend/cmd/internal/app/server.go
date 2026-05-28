package app

import (
	"context"
	"errors"
	"net/http"
	"time"

	"github.com/labstack/echo/v4"
	"github.com/playwright-community/playwright-go"
	"golang.org/x/oauth2"
	"oficina-img/internal/auth"
	"oficina-img/internal/database"
	"oficina-img/internal/repository"
	"oficina-img/internal/routes"
	"oficina-img/internal/service"
)

var discordOAuthEndpoint = oauth2.Endpoint{
	AuthURL:  "https://discord.com/oauth2/authorize",
	TokenURL: "https://discord.com/api/oauth2/token",
}

type Server struct {
	Echo         *echo.Echo
	database     *database.Database
	playwright   *playwright.Playwright
	cardRenderer *service.CardRenderer
}

const shutdownTimeout = 10 * time.Second

func NewServer(cfg Config) (*Server, error) {
	if err := cfg.ValidateAuth(); err != nil {
		return nil, err
	}

	db, err := database.Open(database.Config{Path: cfg.DatabasePath})
	if err != nil {
		return nil, err
	}
	if err := db.Migrate(); err != nil {
		db.Close()
		return nil, err
	}

	if err := playwright.Install(); err != nil {
		db.Close()
		return nil, err
	}

	pw, err := playwright.Run()
	if err != nil {
		db.Close()
		return nil, err
	}

	cardRenderer, err := service.NewCardRenderer(pw)
	if err != nil {
		pw.Stop()
		db.Close()
		return nil, err
	}

	e := echo.New()
	registerRoutes(e, cfg, cardRenderer, newAuthService(cfg, db))
	return &Server{
		Echo:         e,
		database:     db,
		playwright:   pw,
		cardRenderer: cardRenderer,
	}, nil
}

func (s *Server) Start(ctx context.Context, address string) error {
	started := make(chan error, 1)
	go func() {
		started <- s.Echo.Start(address)
	}()

	select {
	case <-ctx.Done():
		shutdownCtx, cancel := context.WithTimeout(context.Background(), shutdownTimeout)
		defer cancel()

		if err := s.Echo.Shutdown(shutdownCtx); err != nil {
			return err
		}
		return ignoreServerClosed(<-started)
	case err := <-started:
		return ignoreServerClosed(err)
	}
}

func (s *Server) Close() error {
	var err error
	if s.cardRenderer != nil {
		err = errors.Join(err, s.cardRenderer.Close())
	}
	if s.playwright != nil {
		err = errors.Join(err, s.playwright.Stop())
	}
	if s.database != nil {
		err = errors.Join(err, s.database.Close())
	}
	return err
}

func ignoreServerClosed(err error) error {
	if errors.Is(err, http.ErrServerClosed) {
		return nil
	}
	return err
}

func registerRoutes(e *echo.Echo, cfg Config, cardRenderer routes.CardRenderer, authService *auth.Service) {
	e.Static("/static", "./static")

	cardHandler := routes.NewCardHandler(cardRenderer)
	externalHandler := routes.NewExternalHandler(service.NewExternalVideoService())
	authHandler := routes.NewAuthHandler(authService, authCookieConfig(cfg))
	adminHandler := routes.NewAdminHandler(authService, authCookieConfig(cfg))

	e.POST("/api/levels/cards", cardHandler.GetLevelCard)
	e.POST("/api/levels/roles", cardHandler.GetLevelsRoles)
	e.GET("/api/external/videos", externalHandler.GetVideo)

	e.GET("/api/auth/discord/start", authHandler.StartDiscordLogin)
	e.GET("/api/auth/discord/callback", authHandler.CompleteDiscordLogin)
	e.GET("/api/auth/me", authHandler.CurrentUser)
	e.POST("/api/auth/logout", authHandler.Logout)

	e.GET("/api/admin/users", adminHandler.ListUsers)
	e.POST("/api/admin/users", adminHandler.AddUser)
	e.DELETE("/api/admin/users/:discord_id", adminHandler.RemoveUser)
}

func newAuthService(cfg Config, db *database.Database) *auth.Service {
	oauthConfig := &oauth2.Config{
		ClientID:     cfg.DiscordClientID,
		ClientSecret: cfg.DiscordClientSecret,
		RedirectURL:  cfg.DiscordRedirectURL,
		Scopes:       []string{auth.DiscordIdentifyScope},
		Endpoint:     discordOAuthEndpoint,
	}

	return auth.NewService(
		auth.NewDiscordClient(oauthConfig),
		repository.NewUserRepository(db.Gorm),
		repository.NewAdminSessionRepository(db.Gorm),
		auth.Config{
			OwnerDiscordID: cfg.OwnerDiscordID,
			SessionSecret:  cfg.SessionSecret,
			SessionTTL:     cfg.SessionTTL,
		},
	)
}

func authCookieConfig(cfg Config) routes.AuthCookieConfig {
	return routes.AuthCookieConfig{
		SessionName:         cfg.SessionCookieName,
		Secure:              cfg.SessionCookieSecure,
		FrontendRedirectURL: cfg.FrontendRedirectURL,
	}
}
