package routes

import (
	"context"
	"errors"
	"net/http"
	"strings"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/auth"
	"oficina-img/internal/repository"
)

type AdminManagementService interface {
	CurrentUser(ctx context.Context, sessionToken string) (*repository.User, error)
	ListAdmins(ctx context.Context, actor *repository.User) ([]repository.User, error)
	AddAdmin(ctx context.Context, actor *repository.User, discordID, username string) (*repository.User, error)
	RemoveAdmin(ctx context.Context, actor *repository.User, discordID string) error
	IsOwner(user *repository.User) bool
}

type AdminHandler struct {
	auth    AdminManagementService
	cookies AuthCookieConfig
}

func NewAdminHandler(authService AdminManagementService, cookies AuthCookieConfig) *AdminHandler {
	return &AdminHandler{auth: authService, cookies: cookies}
}

func (h *AdminHandler) ListUsers(c echo.Context) error {
	actor, ok, err := h.currentUser(c)
	if err != nil {
		return err
	}
	if !ok {
		return c.JSON(http.StatusUnauthorized, authErrorResponse("Missing admin session"))
	}

	users, err := h.auth.ListAdmins(c.Request().Context(), actor)
	if err != nil {
		return writeAdminError(c, err)
	}

	resp := make([]userResponse, 0, len(users))
	for _, user := range users {
		user := user
		resp = append(resp, userResponse{
			DiscordID: user.DiscordID,
			Username:  user.Username,
			IsOwner:   h.auth.IsOwner(&user),
		})
	}
	return c.JSON(http.StatusOK, resp)
}

func (h *AdminHandler) AddUser(c echo.Context) error {
	actor, ok, err := h.currentUser(c)
	if err != nil {
		return err
	}
	if !ok {
		return c.JSON(http.StatusUnauthorized, authErrorResponse("Missing admin session"))
	}

	var req adminUserRequest
	if err := c.Bind(&req); err != nil {
		return c.JSON(http.StatusBadRequest, authErrorResponse("Malformed JSON body"))
	}
	if strings.TrimSpace(req.DiscordID) == "" || strings.TrimSpace(req.Username) == "" {
		return c.JSON(http.StatusBadRequest, authErrorResponse("discord_id and username are required"))
	}

	user, err := h.auth.AddAdmin(c.Request().Context(), actor, req.DiscordID, req.Username)
	if err != nil {
		return writeAdminError(c, err)
	}
	return c.JSON(http.StatusCreated, userResponse{
		DiscordID: user.DiscordID,
		Username:  user.Username,
		IsOwner:   h.auth.IsOwner(user),
	})
}

func (h *AdminHandler) RemoveUser(c echo.Context) error {
	actor, ok, err := h.currentUser(c)
	if err != nil {
		return err
	}
	if !ok {
		return c.JSON(http.StatusUnauthorized, authErrorResponse("Missing admin session"))
	}

	if err := h.auth.RemoveAdmin(c.Request().Context(), actor, c.Param("discord_id")); err != nil {
		return writeAdminError(c, err)
	}
	return c.NoContent(http.StatusNoContent)
}

func (h *AdminHandler) currentUser(c echo.Context) (*repository.User, bool, error) {
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

type adminUserRequest struct {
	DiscordID string `json:"discord_id"`
	Username  string `json:"username"`
}

func writeAdminError(c echo.Context, err error) error {
	switch {
	case errors.Is(err, auth.ErrOwnerOnlyOperation):
		return c.JSON(http.StatusForbidden, authErrorResponse("Only the owner can manage admins"))
	case errors.Is(err, auth.ErrOwnerRemoval):
		return c.JSON(http.StatusBadRequest, authErrorResponse("Owner admin cannot be removed"))
	default:
		return err
	}
}
