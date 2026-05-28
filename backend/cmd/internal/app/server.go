package app

import (
	"context"
	"errors"
	"net/http"
	"time"

	"github.com/labstack/echo/v4"
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

func NewServer() (*Server, error) {
	if err := playwright.Install(); err != nil {
		return nil, err
	}

	pw, err := playwright.Run()
	if err != nil {
		return nil, err
	}

	cardRenderer, err := service.NewCardRenderer(pw)
	if err != nil {
		pw.Stop()
		return nil, err
	}

	e := echo.New()
	registerRoutes(e, cardRenderer)
	return &Server{
		Echo:         e,
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
	return err
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
	externalHandler := routes.NewExternalHandler(service.NewExternalVideoService())

	e.POST("/api/levels/cards", cardHandler.GetLevelCard)
	e.POST("/api/levels/roles", cardHandler.GetLevelsRoles)
	e.GET("/api/external/videos", externalHandler.GetVideo)
}
