package app

import (
	"context"
	"errors"
	"net/http"
	"time"

	"github.com/labstack/echo/v4"
	"github.com/labstack/echo/v4/middleware"
	"github.com/playwright-community/playwright-go"
	"golang.org/x/oauth2"
	"oficina-img/internal/auth"
	"oficina-img/internal/database"
	discordmeta "oficina-img/internal/discord"
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
	if err := cfg.ValidateRuntime(); err != nil {
		return nil, err
	}

	databaseConfig, err := cfg.DatabaseConfig()
	if err != nil {
		return nil, err
	}

	db, err := database.Open(databaseConfig)
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
	registerMiddleware(e, cfg)
	authService := newAuthService(cfg, db)
	serviceAuthenticator := auth.NewServiceAuthenticator(repository.NewBotClientRepository(db.Gorm))
	discordClient, err := discordmeta.NewClient(cfg.DiscordBotToken)
	if err != nil {
		cardRenderer.Close()
		pw.Stop()
		db.Close()
		return nil, err
	}
	metadataService := discordmeta.NewMetadataService(discordClient)
	registerRoutes(e, cfg, db, cardRenderer, authService, serviceAuthenticator, metadataService)
	return &Server{
		Echo:         e,
		database:     db,
		playwright:   pw,
		cardRenderer: cardRenderer,
	}, nil
}

func registerMiddleware(e *echo.Echo, cfg Config) {
	e.Use(middleware.RequestID())
	e.Use(middleware.Recover())
	e.Use(middleware.BodyLimit(cfg.BodyLimit))
	e.Use(middleware.LoggerWithConfig(middleware.LoggerConfig{
		Format: `{"time":"${time_rfc3339}","id":"${id}","remote_ip":"${remote_ip}","host":"${host}","method":"${method}","uri":"${uri}","status":${status},"latency":"${latency_human}","bytes_in":${bytes_in},"bytes_out":${bytes_out}}` + "\n",
	}))
	e.Use(middleware.CORSWithConfig(middleware.CORSConfig{
		AllowOrigins: []string{"*"},
		AllowMethods: []string{http.MethodGet, http.MethodPost, http.MethodDelete, http.MethodOptions},
		AllowHeaders: []string{echo.HeaderOrigin, echo.HeaderContentType, echo.HeaderAccept, echo.HeaderAuthorization, echo.HeaderXCSRFToken},
	}))
	e.Use(middleware.RateLimiter(middleware.NewRateLimiterMemoryStore(20)))
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

func registerRoutes(e *echo.Echo, cfg Config, db *database.Database, cardRenderer routes.CardRenderer, authService *auth.Service, serviceAuthenticator *auth.ServiceAuthenticator, metadataService routes.DiscordMetadataService) {
	e.Static("/static", "./static")

	cardHandler := routes.NewCardHandler(cardRenderer)
	healthHandler := routes.NewHealthHandler()
	externalHandler := routes.NewExternalHandler(service.NewExternalVideoService())
	authHandler := routes.NewAuthHandler(authService, authCookieConfig(cfg))
	adminHandler := routes.NewAdminHandler(authService, authCookieConfig(cfg))
	serviceHandler := routes.NewServiceHandler()
	batchRepo := repository.NewEventBatchRepository(db.Gorm)
	messageLogRepo := repository.NewMessageLogRepository(db.Gorm)
	punishmentRepo := repository.NewPunishmentRepository(db.Gorm)
	registrationRepo := repository.NewRegistrationRepository(db.Gorm)
	heartbeatRepo := repository.NewSyncHeartbeatRepository(db.Gorm)
	configRepo := repository.NewConfigVersionRepository(db.Gorm)
	auditRepo := repository.NewAuditActionRepository(db.Gorm)
	ingestHandler := routes.NewIngestHandler(batchRepo, messageLogRepo, punishmentRepo, registrationRepo, heartbeatRepo)
	dashboardHandler := routes.NewDashboardHandler(
		authService,
		authCookieConfig(cfg),
		messageLogRepo,
		punishmentRepo,
		registrationRepo,
		heartbeatRepo,
		auditRepo,
	)
	configHandler := routes.NewConfigHandler(authService, authCookieConfig(cfg), configRepo, auditRepo)
	configSyncHandler := routes.NewConfigSyncHandler(configRepo)
	discordMetadataHandler := routes.NewDiscordMetadataHandler(authService, authCookieConfig(cfg), metadataService)

	e.GET("/health", healthHandler.Check)

	e.POST("/api/levels/cards", cardHandler.GetLevelCard)
	e.POST("/api/levels/roles", cardHandler.GetLevelsRoles)
	e.GET("/api/external/videos", externalHandler.GetVideo)

	e.GET("/api/auth/discord/start", authHandler.StartDiscordLogin)
	e.GET("/api/auth/discord/callback", authHandler.CompleteDiscordLogin)
	e.GET("/api/auth/me", authHandler.CurrentUser)
	e.POST("/api/auth/logout", authHandler.Logout, csrfMiddleware(cfg))

	e.GET("/api/admin/users", adminHandler.ListUsers)
	e.POST("/api/admin/users", adminHandler.AddUser, csrfMiddleware(cfg))
	e.DELETE("/api/admin/users/:discord_id", adminHandler.RemoveUser, csrfMiddleware(cfg))

	e.GET("/api/dashboard/message-logs", dashboardHandler.MessageLogs)
	e.GET("/api/dashboard/punishments", dashboardHandler.Punishments)
	e.GET("/api/dashboard/registrations", dashboardHandler.Registrations)
	e.GET("/api/dashboard/sync-health", dashboardHandler.SyncHealth)
	e.GET("/api/dashboard/audit-actions", dashboardHandler.AuditActions)
	e.GET("/api/dashboard/configs", configHandler.ListConfigs)
	e.POST("/api/dashboard/configs", configHandler.CreateConfig, csrfMiddleware(cfg))
	e.GET("/api/dashboard/discord/guilds/:guild_id", discordMetadataHandler.Guild)
	e.GET("/api/dashboard/discord/channels/:channel_id", discordMetadataHandler.Channel)
	e.GET("/api/dashboard/discord/guilds/:guild_id/roles", discordMetadataHandler.GuildRoles)
	e.GET("/api/dashboard/discord/users/:user_id", discordMetadataHandler.User)

	serviceGroup := e.Group("/api/service", routes.ServiceAuthMiddleware(serviceAuthenticator))
	serviceGroup.GET("/me", serviceHandler.Me)
	serviceGroup.POST("/batches/message-logs", ingestHandler.MessageLogs)
	serviceGroup.POST("/batches/punishments", ingestHandler.Punishments)
	serviceGroup.POST("/batches/registrations", ingestHandler.Registrations)
	serviceGroup.POST("/sync-heartbeat", ingestHandler.SyncHeartbeat)
	serviceGroup.GET("/configs/pending", configSyncHandler.Pending)
	serviceGroup.POST("/configs/:version_id/ack", configSyncHandler.Ack)
}

func csrfMiddleware(cfg Config) echo.MiddlewareFunc {
	return middleware.CSRFWithConfig(middleware.CSRFConfig{
		TokenLookup:    "header:" + echo.HeaderXCSRFToken,
		CookieName:     "oficina_csrf",
		CookiePath:     "/",
		CookieHTTPOnly: true,
		CookieSecure:   cfg.SessionCookieSecure,
		CookieSameSite: http.SameSiteLaxMode,
	})
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
