package discord

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"strings"
	"time"

	"oficina-img/internal/contract"
)

type BotClient struct {
	apiBaseURL string
	token      string
	httpClient *http.Client
}

type GuildChannel = contract.GuildChannelResponse
type GuildRole = contract.GuildRoleResponse

func NewBotClient(apiBaseURL, token string) *BotClient {
	return &BotClient{apiBaseURL: strings.TrimRight(apiBaseURL, "/"), token: token, httpClient: &http.Client{Timeout: 10 * time.Second}}
}

func (c *BotClient) GuildChannels(ctx context.Context, guildID string) ([]GuildChannel, error) {
	var channels []GuildChannel
	return channels, c.get(ctx, "/guilds/"+guildID+"/channels", &channels)
}

func (c *BotClient) GuildRoles(ctx context.Context, guildID string) ([]GuildRole, error) {
	var roles []GuildRole
	return roles, c.get(ctx, "/guilds/"+guildID+"/roles", &roles)
}

func (c *BotClient) get(ctx context.Context, path string, out any) error {
	if c.token == "" {
		return fmt.Errorf("discord bot token is not configured")
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, c.apiBaseURL+path, nil)
	if err != nil {
		return err
	}
	req.Header.Set("Authorization", "Bot "+c.token)
	req.Header.Set("Accept", "application/json")
	req.Header.Set("User-Agent", "OficinaServices Dashboard")
	resp, err := c.httpClient.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		return fmt.Errorf("discord request failed with status %d", resp.StatusCode)
	}
	return json.NewDecoder(resp.Body).Decode(out)
}
