package routes

import (
	"context"
	"net/http"
	"strconv"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/repository"
)

type MessageLogReader interface {
	ListRecent(ctx context.Context, limit int) ([]repository.MessageLog, error)
}

type PunishmentReader interface {
	ListRecent(ctx context.Context, limit int) ([]repository.Punishment, error)
}

type RegistrationReader interface {
	ListRecent(ctx context.Context, limit int) ([]repository.Registration, error)
}

type SyncHeartbeatReader interface {
	ListLatest(ctx context.Context, limit int) ([]repository.SyncHeartbeat, error)
}

type AuditActionReader interface {
	ListRecent(ctx context.Context, limit int) ([]repository.AuditAction, error)
}

type DashboardAuthService interface {
	CurrentUser(ctx context.Context, sessionToken string) (*repository.User, error)
}

type DashboardHandler struct {
	auth          DashboardAuthService
	cookies       AuthCookieConfig
	messageLogs   MessageLogReader
	punishments   PunishmentReader
	registrations RegistrationReader
	heartbeats    SyncHeartbeatReader
	auditActions  AuditActionReader
}

func NewDashboardHandler(
	authService DashboardAuthService,
	cookies AuthCookieConfig,
	messageLogs MessageLogReader,
	punishments PunishmentReader,
	registrations RegistrationReader,
	heartbeats SyncHeartbeatReader,
	auditActions AuditActionReader,
) *DashboardHandler {
	return &DashboardHandler{
		auth:          authService,
		cookies:       cookies,
		messageLogs:   messageLogs,
		punishments:   punishments,
		registrations: registrations,
		heartbeats:    heartbeats,
		auditActions:  auditActions,
	}
}

func (h *DashboardHandler) MessageLogs(c echo.Context) error {
	if ok, err := h.requireAdmin(c); !ok || err != nil {
		return err
	}
	limit, err := queryLimit(c)
	if err != nil {
		return c.JSON(http.StatusBadRequest, authErrorResponse("Invalid limit"))
	}
	logs, err := h.messageLogs.ListRecent(c.Request().Context(), limit)
	if err != nil {
		return err
	}
	return c.JSON(http.StatusOK, logs)
}

func (h *DashboardHandler) Punishments(c echo.Context) error {
	if ok, err := h.requireAdmin(c); !ok || err != nil {
		return err
	}
	limit, err := queryLimit(c)
	if err != nil {
		return c.JSON(http.StatusBadRequest, authErrorResponse("Invalid limit"))
	}
	punishments, err := h.punishments.ListRecent(c.Request().Context(), limit)
	if err != nil {
		return err
	}
	return c.JSON(http.StatusOK, punishments)
}

func (h *DashboardHandler) Registrations(c echo.Context) error {
	if ok, err := h.requireAdmin(c); !ok || err != nil {
		return err
	}
	limit, err := queryLimit(c)
	if err != nil {
		return c.JSON(http.StatusBadRequest, authErrorResponse("Invalid limit"))
	}
	registrations, err := h.registrations.ListRecent(c.Request().Context(), limit)
	if err != nil {
		return err
	}
	return c.JSON(http.StatusOK, registrations)
}

func (h *DashboardHandler) SyncHealth(c echo.Context) error {
	if ok, err := h.requireAdmin(c); !ok || err != nil {
		return err
	}
	limit, err := queryLimit(c)
	if err != nil {
		return c.JSON(http.StatusBadRequest, authErrorResponse("Invalid limit"))
	}
	heartbeats, err := h.heartbeats.ListLatest(c.Request().Context(), limit)
	if err != nil {
		return err
	}
	return c.JSON(http.StatusOK, heartbeats)
}

func (h *DashboardHandler) AuditActions(c echo.Context) error {
	if ok, err := h.requireAdmin(c); !ok || err != nil {
		return err
	}
	limit, err := queryLimit(c)
	if err != nil {
		return c.JSON(http.StatusBadRequest, authErrorResponse("Invalid limit"))
	}
	actions, err := h.auditActions.ListRecent(c.Request().Context(), limit)
	if err != nil {
		return err
	}
	return c.JSON(http.StatusOK, actions)
}

func (h *DashboardHandler) requireAdmin(c echo.Context) (bool, error) {
	sessionCookie, err := c.Cookie(h.cookies.SessionName)
	if err != nil {
		return false, c.JSON(http.StatusUnauthorized, authErrorResponse("Missing admin session"))
	}
	if _, err := h.auth.CurrentUser(c.Request().Context(), sessionCookie.Value); err != nil {
		return false, c.JSON(http.StatusUnauthorized, authErrorResponse("Invalid admin session"))
	}
	return true, nil
}

func queryLimit(c echo.Context) (int, error) {
	value := c.QueryParam("limit")
	if value == "" {
		return 50, nil
	}
	limit, err := strconv.Atoi(value)
	if err != nil || limit <= 0 || limit > 100 {
		return 0, strconv.ErrSyntax
	}
	return limit, nil
}
