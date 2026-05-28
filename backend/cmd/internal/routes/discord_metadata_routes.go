package routes

import (
	"context"
	"net/http"

	"github.com/labstack/echo/v4"
	discordmeta "oficina-img/internal/discord"
)

type DiscordMetadataService interface {
	Guild(ctx context.Context, guildID string) (discordmeta.GuildMetadata, error)
	Channel(ctx context.Context, channelID string) (discordmeta.ChannelMetadata, error)
	GuildRoles(ctx context.Context, guildID string) ([]discordmeta.RoleMetadata, error)
	User(ctx context.Context, userID string) (discordmeta.UserMetadata, error)
}

type DiscordMetadataHandler struct {
	auth     DashboardAuthService
	cookies  AuthCookieConfig
	metadata DiscordMetadataService
}

func NewDiscordMetadataHandler(authService DashboardAuthService, cookies AuthCookieConfig, metadata DiscordMetadataService) *DiscordMetadataHandler {
	return &DiscordMetadataHandler{auth: authService, cookies: cookies, metadata: metadata}
}

func (h *DiscordMetadataHandler) Guild(c echo.Context) error {
	if ok, err := h.requireAdmin(c); !ok || err != nil {
		return err
	}
	guild, err := h.metadata.Guild(c.Request().Context(), c.Param("guild_id"))
	if err != nil {
		return err
	}
	return c.JSON(http.StatusOK, guild)
}

func (h *DiscordMetadataHandler) Channel(c echo.Context) error {
	if ok, err := h.requireAdmin(c); !ok || err != nil {
		return err
	}
	channel, err := h.metadata.Channel(c.Request().Context(), c.Param("channel_id"))
	if err != nil {
		return err
	}
	return c.JSON(http.StatusOK, channel)
}

func (h *DiscordMetadataHandler) GuildRoles(c echo.Context) error {
	if ok, err := h.requireAdmin(c); !ok || err != nil {
		return err
	}
	roles, err := h.metadata.GuildRoles(c.Request().Context(), c.Param("guild_id"))
	if err != nil {
		return err
	}
	return c.JSON(http.StatusOK, roles)
}

func (h *DiscordMetadataHandler) User(c echo.Context) error {
	if ok, err := h.requireAdmin(c); !ok || err != nil {
		return err
	}
	user, err := h.metadata.User(c.Request().Context(), c.Param("user_id"))
	if err != nil {
		return err
	}
	return c.JSON(http.StatusOK, user)
}

func (h *DiscordMetadataHandler) requireAdmin(c echo.Context) (bool, error) {
	sessionCookie, err := c.Cookie(h.cookies.SessionName)
	if err != nil {
		return false, c.JSON(http.StatusUnauthorized, authErrorResponse("Missing admin session"))
	}
	if _, err := h.auth.CurrentUser(c.Request().Context(), sessionCookie.Value); err != nil {
		return false, c.JSON(http.StatusUnauthorized, authErrorResponse("Invalid admin session"))
	}
	return true, nil
}
