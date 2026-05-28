package routes

import (
	"context"
	"encoding/json"
	"net/http"
	"strings"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/repository"
)

type ConfigVersionStore interface {
	Create(ctx context.Context, version *repository.ConfigVersion) error
	ListRecent(ctx context.Context, limit int) ([]repository.ConfigVersion, error)
}

type AuditActionStore interface {
	Create(ctx context.Context, action *repository.AuditAction) error
}

type ConfigHandler struct {
	auth    DashboardAuthService
	cookies AuthCookieConfig
	configs ConfigVersionStore
	audit   AuditActionStore
}

func NewConfigHandler(authService DashboardAuthService, cookies AuthCookieConfig, configs ConfigVersionStore, audit AuditActionStore) *ConfigHandler {
	return &ConfigHandler{auth: authService, cookies: cookies, configs: configs, audit: audit}
}

func (h *ConfigHandler) ListConfigs(c echo.Context) error {
	user, ok, err := h.currentAdmin(c)
	if err != nil {
		return err
	}
	_ = user
	if !ok {
		return c.JSON(http.StatusUnauthorized, authErrorResponse("Missing admin session"))
	}

	limit, err := queryLimit(c)
	if err != nil {
		return c.JSON(http.StatusBadRequest, authErrorResponse("Invalid limit"))
	}
	configs, err := h.configs.ListRecent(c.Request().Context(), limit)
	if err != nil {
		return err
	}
	return c.JSON(http.StatusOK, configs)
}

func (h *ConfigHandler) CreateConfig(c echo.Context) error {
	user, ok, err := h.currentAdmin(c)
	if err != nil {
		return err
	}
	if !ok {
		return c.JSON(http.StatusUnauthorized, authErrorResponse("Missing admin session"))
	}

	var req configWriteRequest
	if err := c.Bind(&req); err != nil {
		return c.JSON(http.StatusBadRequest, authErrorResponse("Malformed JSON body"))
	}
	if strings.TrimSpace(req.Scope) == "" || strings.TrimSpace(req.Key) == "" || len(req.Value) == 0 || !json.Valid(req.Value) {
		return c.JSON(http.StatusBadRequest, authErrorResponse("scope, key, and valid JSON value are required"))
	}

	version := &repository.ConfigVersion{
		Scope:              strings.TrimSpace(req.Scope),
		Key:                strings.TrimSpace(req.Key),
		ValueJSON:          string(req.Value),
		CreatedByDiscordID: &user.DiscordID,
	}
	if err := h.configs.Create(c.Request().Context(), version); err != nil {
		return err
	}

	targetID := version.Scope + ":" + version.Key
	if err := h.audit.Create(c.Request().Context(), &repository.AuditAction{
		ActorDiscordID: &user.DiscordID,
		Action:         "config.create",
		TargetType:     "config",
		TargetID:       &targetID,
		MetadataJSON:   string(req.Value),
	}); err != nil {
		return err
	}

	return c.JSON(http.StatusCreated, version)
}

func (h *ConfigHandler) currentAdmin(c echo.Context) (*repository.User, bool, error) {
	sessionCookie, err := c.Cookie(h.cookies.SessionName)
	if err != nil {
		return nil, false, nil
	}
	user, err := h.auth.CurrentUser(c.Request().Context(), sessionCookie.Value)
	if err != nil {
		return nil, false, nil
	}
	return user, true, nil
}

type configWriteRequest struct {
	Scope string          `json:"scope"`
	Key   string          `json:"key"`
	Value json.RawMessage `json:"value"`
}
