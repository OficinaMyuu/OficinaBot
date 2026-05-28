package auth

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"

	"golang.org/x/oauth2"
)

const discordCurrentUserURL = "https://discord.com/api/users/@me"

type OAuthConfig interface {
	AuthCodeURL(state string, opts ...oauth2.AuthCodeOption) string
	Exchange(ctx context.Context, code string, opts ...oauth2.AuthCodeOption) (*oauth2.Token, error)
	Client(ctx context.Context, token *oauth2.Token) *http.Client
}

type DiscordClient struct {
	oauth OAuthConfig
}

func NewDiscordClient(oauth OAuthConfig) *DiscordClient {
	return &DiscordClient{oauth: oauth}
}

func (c *DiscordClient) AuthCodeURL(state string) string {
	return c.oauth.AuthCodeURL(state, oauth2.AccessTypeOnline)
}

func (c *DiscordClient) Exchange(ctx context.Context, code string) (*oauth2.Token, error) {
	return c.oauth.Exchange(ctx, code)
}

func (c *DiscordClient) CurrentUser(ctx context.Context, token *oauth2.Token) (*DiscordUser, error) {
	req, err := http.NewRequestWithContext(ctx, http.MethodGet, discordCurrentUserURL, nil)
	if err != nil {
		return nil, err
	}

	resp, err := c.oauth.Client(ctx, token).Do(req)
	if err != nil {
		return nil, err
	}
	defer resp.Body.Close()

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("discord current user returned status %d", resp.StatusCode)
	}

	var user DiscordUser
	if err := json.NewDecoder(resp.Body).Decode(&user); err != nil {
		return nil, err
	}
	return &user, nil
}
