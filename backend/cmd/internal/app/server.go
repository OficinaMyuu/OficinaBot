package app

import (
	"context"
	"errors"
	"fmt"
	"net/http"
	"os"
	"path/filepath"
	"time"

	"github.com/labstack/echo/v4"
	"github.com/labstack/echo/v4/middleware"
	"github.com/playwright-community/playwright-go"
	"oficina-img/internal/routes"
	"oficina-img/internal/service"
)

type Server struct {
	Echo         *echo.Echo
	playwright   *playwright.Playwright
	cardRenderer *service.CardRenderer
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
	registerRoutes(e, cardRenderer)
	return &Server{
		Echo:         e,
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
		AllowMethods: []string{http.MethodGet, http.MethodPost, http.MethodOptions},
		AllowHeaders: []string{echo.HeaderOrigin, echo.HeaderContentType, echo.HeaderAccept},
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

func registerRoutes(e *echo.Echo, cardRenderer routes.CardRenderer) {
	e.Static("/static", "./static")

	cardHandler := routes.NewCardHandler(cardRenderer)
	healthHandler := routes.NewHealthHandler()

	e.GET("/health", healthHandler.Check)

	e.POST("/api/levels/cards", cardHandler.GetLevelCard)
	e.POST("/api/levels/roles", cardHandler.GetLevelsRoles)
}
