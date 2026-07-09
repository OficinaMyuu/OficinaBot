package app

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"net/http"
	"os"
	"path/filepath"
	"time"

	"github.com/labstack/echo/v4"
	"github.com/labstack/echo/v4/middleware"
	"github.com/mxschmitt/playwright-go"
	"oficina-img/internal/discord"
	"oficina-img/internal/routes"
	"oficina-img/internal/service"
	"oficina-img/internal/store"
)

type Server struct {
	Echo         *echo.Echo
	playwright   *playwright.Playwright
	cardRenderer *service.CardRenderer
	db           *sql.DB
}

const shutdownTimeout = 10 * time.Second
const defaultPlaywrightDriverPath = "/var/lib/oficina/backend/playwright-driver"

func NewServer(cfg Config) (*Server, error) {
	if err := cfg.ValidateRuntime(); err != nil {
		return nil, err
	}

	runOptions := playwrightRunOptions()
	if err := ensurePlaywrightDriver(runOptions.DriverDirectory); err != nil {
		return nil, fmt.Errorf("playwright driver is not available: %w", err)
	}

	pw, err := playwright.Run(runOptions)
	if err != nil {
		return nil, err
	}

	cardRenderer, err := service.NewCardRenderer(pw)
	if err != nil {
		pw.Stop()
		return nil, err
	}

	e := echo.New()
	registerMiddleware(e, cfg)
	db, birthdayRepository, ticketRepository, oauthClient, missingConfig, err := dashboardRuntime(cfg)
	if err != nil {
		cardRenderer.Close()
		pw.Stop()
		return nil, err
	}
	registerRoutes(e, cfg, cardRenderer, birthdayRepository, ticketRepository, oauthClient, missingConfig)
	return &Server{
		Echo:         e,
		playwright:   pw,
		cardRenderer: cardRenderer,
		db:           db,
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
		AllowOriginFunc:  allowedOriginFunc(cfg.Dashboard.AllowedCORSOrigins()),
		AllowMethods:     []string{http.MethodDelete, http.MethodGet, http.MethodHead, http.MethodOptions, http.MethodPatch, http.MethodPost, http.MethodPut},
		AllowCredentials: true,
	}))
	e.Use(middleware.RateLimiter(middleware.NewRateLimiterMemoryStore(20)))
}

func allowedOriginFunc(allowedOrigins []string) func(string) (bool, error) {
	allowed := make(map[string]struct{}, len(allowedOrigins))
	for _, origin := range allowedOrigins {
		allowed[origin] = struct{}{}
	}

	return func(origin string) (bool, error) {
		_, ok := allowed[origin]
		return ok, nil
	}
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
	if s.db != nil {
		err = errors.Join(err, s.db.Close())
	}
	return err
}

func playwrightRunOptions() *playwright.RunOptions {
	driverPath := os.Getenv("PLAYWRIGHT_DRIVER_PATH")
	if driverPath == "" {
		driverPath = defaultPlaywrightDriverPath
	}
	return &playwright.RunOptions{
		DriverDirectory:     driverPath,
		SkipInstallBrowsers: true,
	}
}

func ensurePlaywrightDriver(driverDirectory string) error {
	if driverDirectory == "" {
		return errors.New("driver directory is empty")
	}

	for _, path := range []string{
		filepath.Join(driverDirectory, "node"),
		filepath.Join(driverDirectory, "package", "cli.js"),
	} {
		info, err := os.Stat(path)
		if err != nil {
			if errors.Is(err, os.ErrNotExist) {
				return fmt.Errorf("missing required driver file %s", path)
			}
			return fmt.Errorf("cannot inspect required driver file %s: %w", path, err)
		}
		if info.IsDir() {
			return fmt.Errorf("required driver file %s is a directory", path)
		}
	}

	return nil
}

func ignoreServerClosed(err error) error {
	if errors.Is(err, http.ErrServerClosed) {
		return nil
	}
	return err
}

func registerRoutes(
	e *echo.Echo,
	cfg Config,
	cardRenderer routes.CardRenderer,
	birthdayRepository routes.BirthdayRepository,
	ticketRepository routes.TicketRepository,
	oauthClient routes.DiscordOAuthClient,
	missingConfig []string,
) {
	e.Static("/static", "./static")

	cardHandler := routes.NewCardHandler(cardRenderer)
	healthHandler := routes.NewHealthHandler()

	e.GET("/health", healthHandler.Check)

	e.POST("/api/levels/cards", cardHandler.GetLevelCard)
	e.POST("/api/levels/roles", cardHandler.GetLevelsRoles)
	e.POST("/levels/cards", cardHandler.GetLevelCard)
	e.POST("/levels/roles", cardHandler.GetLevelsRoles)

	routes.RegisterDashboardRoutes(e, routes.DashboardRoutesConfig{
		AuthConfig: routes.DashboardAuthConfig{
			PublicAPIBaseURL: cfg.Dashboard.PublicAPIBaseURL,
			FrontendBaseURL:  cfg.Dashboard.FrontendBaseURL,
			AuthorizeURL:     cfg.Dashboard.DiscordAuthorizeURL,
			ClientID:         cfg.Dashboard.DiscordClientID,
			GuildID:          cfg.Dashboard.DiscordGuildID,
			CookieSecure:     cfg.Dashboard.CookieSecure(),
			MissingConfig:    missingConfig,
		},
		OAuthClient: oauthClient,
		Sessions:    routes.NewSessionStore(),
		Birthdays:   birthdayRepository,
		Tickets:     ticketRepository,
	})
}

func dashboardRuntime(cfg Config) (*sql.DB, routes.BirthdayRepository, routes.TicketRepository, routes.DiscordOAuthClient, []string, error) {
	missingConfig := cfg.MissingDashboardConfig()
	if len(missingConfig) > 0 {
		return nil, nil, nil, nil, missingConfig, nil
	}

	dsn, err := cfg.Database.FormatDSN()
	if err != nil {
		return nil, nil, nil, nil, nil, fmt.Errorf("dashboard database config is invalid: %w", err)
	}

	db, err := sql.Open("mysql", dsn)
	if err != nil {
		return nil, nil, nil, nil, nil, fmt.Errorf("open dashboard database: %w", err)
	}
	db.SetMaxOpenConns(10)
	db.SetMaxIdleConns(5)
	db.SetConnMaxLifetime(30 * time.Minute)

	pingCtx, cancel := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancel()
	if err := db.PingContext(pingCtx); err != nil {
		db.Close()
		return nil, nil, nil, nil, nil, fmt.Errorf("ping dashboard database: %w", err)
	}

	return db,
		store.NewBirthdayRepository(db),
		store.NewTicketRepository(db),
		discord.NewOAuthClient(cfg.Dashboard.DiscordAPIBaseURL, cfg.Dashboard.DiscordClientID, cfg.Dashboard.DiscordClientSecret),
		nil,
		nil
}
