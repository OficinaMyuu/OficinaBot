package routes

import (
	"context"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/labstack/echo/v4"
	discordmeta "oficina-img/internal/discord"
	"oficina-img/internal/repository"
)

func TestDiscordMetadataHandlerRequiresAdmin(t *testing.T) {
	e := echo.New()
	handler := NewDiscordMetadataHandler(&stubDashboardAuth{}, AuthCookieConfig{SessionName: "session"}, &stubDiscordMetadataService{})
	rec := httptest.NewRecorder()
	req := httptest.NewRequest(http.MethodGet, "/api/dashboard/discord/guilds/guild", nil)

	err := handler.Guild(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusUnauthorized {
		t.Fatalf("expected status %d, got %d", http.StatusUnauthorized, rec.Code)
	}
}

func TestDiscordMetadataHandlerReturnsGuild(t *testing.T) {
	e := echo.New()
	metadata := &stubDiscordMetadataService{guild: discordmeta.GuildMetadata{ID: "guild", Name: "Oficina"}}
	handler := newTestDiscordMetadataHandler(metadata)
	rec := httptest.NewRecorder()
	req := dashboardRequest(http.MethodGet, "/api/dashboard/discord/guilds/guild")
	ctx := e.NewContext(req, rec)
	ctx.SetParamNames("guild_id")
	ctx.SetParamValues("guild")

	err := handler.Guild(ctx)

	if err != nil {
		t.Fatalf("expected no echo error, got %v", err)
	}
	if rec.Code != http.StatusOK {
		t.Fatalf("expected status %d, got %d", http.StatusOK, rec.Code)
	}
	if !strings.Contains(rec.Body.String(), `"name":"Oficina"`) {
		t.Fatalf("expected Oficina guild, got %s", rec.Body.String())
	}
}

func TestDiscordMetadataHandlerReturnsChannelRolesAndUser(t *testing.T) {
	e := echo.New()
	metadata := &stubDiscordMetadataService{
		channel: discordmeta.ChannelMetadata{ID: "channel", Name: "general"},
		roles:   []discordmeta.RoleMetadata{{ID: "role", Name: "Admin"}},
		user:    discordmeta.UserMetadata{ID: "user", Username: "myuu"},
	}
	handler := newTestDiscordMetadataHandler(metadata)

	tests := []struct {
		name       string
		handler    func(echo.Context) error
		paramName  string
		paramValue string
		want       string
	}{
		{name: "channel", handler: handler.Channel, paramName: "channel_id", paramValue: "channel", want: `"name":"general"`},
		{name: "roles", handler: handler.GuildRoles, paramName: "guild_id", paramValue: "guild", want: `"name":"Admin"`},
		{name: "user", handler: handler.User, paramName: "user_id", paramValue: "user", want: `"username":"myuu"`},
	}

	for _, tt := range tests {
		t.Run(tt.name, func(t *testing.T) {
			rec := httptest.NewRecorder()
			ctx := e.NewContext(dashboardRequest(http.MethodGet, "/metadata"), rec)
			ctx.SetParamNames(tt.paramName)
			ctx.SetParamValues(tt.paramValue)
			if err := tt.handler(ctx); err != nil {
				t.Fatalf("expected no echo error, got %v", err)
			}
			if rec.Code != http.StatusOK {
				t.Fatalf("expected status %d, got %d", http.StatusOK, rec.Code)
			}
			if !strings.Contains(rec.Body.String(), tt.want) {
				t.Fatalf("expected %s in response, got %s", tt.want, rec.Body.String())
			}
		})
	}
}

func newTestDiscordMetadataHandler(metadata *stubDiscordMetadataService) *DiscordMetadataHandler {
	return NewDiscordMetadataHandler(
		&stubDashboardAuth{user: &repository.User{DiscordID: "100", Username: "Leonardo"}},
		AuthCookieConfig{SessionName: "session"},
		metadata,
	)
}

type stubDiscordMetadataService struct {
	guild   discordmeta.GuildMetadata
	channel discordmeta.ChannelMetadata
	roles   []discordmeta.RoleMetadata
	user    discordmeta.UserMetadata
}

func (s *stubDiscordMetadataService) Guild(_ context.Context, _ string) (discordmeta.GuildMetadata, error) {
	return s.guild, nil
}

func (s *stubDiscordMetadataService) Channel(_ context.Context, _ string) (discordmeta.ChannelMetadata, error) {
	return s.channel, nil
}

func (s *stubDiscordMetadataService) GuildRoles(_ context.Context, _ string) ([]discordmeta.RoleMetadata, error) {
	return s.roles, nil
}

func (s *stubDiscordMetadataService) User(_ context.Context, _ string) (discordmeta.UserMetadata, error) {
	return s.user, nil
}
