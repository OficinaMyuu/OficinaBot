package handler

import (
	"context"
	"net/http"
	"sort"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/contract"
)

type GuildDirectoryClient interface {
	GuildChannels(context.Context, string) ([]contract.GuildChannelResponse, error)
	GuildRoles(context.Context, string) ([]contract.GuildRoleResponse, error)
}

type GuildDirectoryHandler struct {
	client  GuildDirectoryClient
	guildID string
}

func NewGuildDirectoryHandler(client GuildDirectoryClient, guildID string) *GuildDirectoryHandler {
	return &GuildDirectoryHandler{client: client, guildID: guildID}
}

func (h *GuildDirectoryHandler) Get(c echo.Context) error {
	channels, err := h.client.GuildChannels(c.Request().Context(), h.guildID)
	if err != nil {
		return jsonError(c, http.StatusBadGateway, "Could not load guild channels")
	}
	roles, err := h.client.GuildRoles(c.Request().Context(), h.guildID)
	if err != nil {
		return jsonError(c, http.StatusBadGateway, "Could not load guild roles")
	}
	response := contract.GuildDirectoryResponse{Channels: channels, Roles: roles}
	sort.Slice(response.Channels, func(i, j int) bool { return response.Channels[i].Name < response.Channels[j].Name })
	sort.Slice(response.Roles, func(i, j int) bool { return response.Roles[i].Name < response.Roles[j].Name })
	return c.JSON(http.StatusOK, response)
}
