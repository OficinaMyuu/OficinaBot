package discord

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"strings"
	"time"
)

type OAuthClient struct {
	apiBaseURL   string
	clientID     string
	clientSecret string
	httpClient   *http.Client
}

type User struct {
	ID         string  `json:"id"`
	Username   string  `json:"username"`
	GlobalName *string `json:"global_name"`
	Avatar     *string `json:"avatar"`
}

type Guild struct {
	ID          string  `json:"id"`
	Name        string  `json:"name"`
	Icon        *string `json:"icon"`
	Owner       bool    `json:"owner"`
	Permissions string  `json:"permissions"`
}

func NewOAuthClient(apiBaseURL, clientID, clientSecret string) *OAuthClient {
	return &OAuthClient{
		apiBaseURL:   strings.TrimRight(apiBaseURL, "/"),
		clientID:     clientID,
		clientSecret: clientSecret,
		httpClient: &http.Client{
			Timeout: 10 * time.Second,
		},
	}
}

func (c *OAuthClient) Exchange(ctx context.Context, code, redirectURI string) (string, error) {
	form := url.Values{}
	form.Set("client_id", c.clientID)
	form.Set("client_secret", c.clientSecret)
	form.Set("grant_type", "authorization_code")
	form.Set("code", code)
	form.Set("redirect_uri", redirectURI)

	req, err := http.NewRequestWithContext(ctx, http.MethodPost, c.apiBaseURL+"/oauth2/token", strings.NewReader(form.Encode()))
	if err != nil {
		return "", err
	}
	req.Header.Set("Content-Type", "application/x-www-form-urlencoded")

	var body struct {
		AccessToken string `json:"access_token"`
	}
	if err := c.doJSON(req, &body); err != nil {
		return "", err
	}
	if body.AccessToken == "" {
		return "", fmt.Errorf("discord token response did not include access token")
	}
	return body.AccessToken, nil
}

func (c *OAuthClient) CurrentUser(ctx context.Context, accessToken string) (User, error) {
	var user User
	req, err := c.authenticatedRequest(ctx, http.MethodGet, "/users/@me", accessToken)
	if err != nil {
		return User{}, err
	}
	if err := c.doJSON(req, &user); err != nil {
		return User{}, err
	}
	return user, nil
}

func (c *OAuthClient) CurrentGuilds(ctx context.Context, accessToken string) ([]Guild, error) {
	var guilds []Guild
	req, err := c.authenticatedRequest(ctx, http.MethodGet, "/users/@me/guilds", accessToken)
	if err != nil {
		return nil, err
	}
	if err := c.doJSON(req, &guilds); err != nil {
		return nil, err
	}
	return guilds, nil
}

func (c *OAuthClient) authenticatedRequest(ctx context.Context, method, path, accessToken string) (*http.Request, error) {
	req, err := http.NewRequestWithContext(ctx, method, c.apiBaseURL+path, nil)
	if err != nil {
		return nil, err
	}
	req.Header.Set("Authorization", "Bearer "+accessToken)
	return req, nil
}

func (c *OAuthClient) doJSON(req *http.Request, out any) error {
	req.Header.Set("Accept", "application/json")
	req.Header.Set("User-Agent", "OficinaServices Dashboard")

	resp, err := c.httpClient.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()

	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		var discordErr struct {
			Message string `json:"message"`
		}
		if err := json.NewDecoder(resp.Body).Decode(&discordErr); err == nil && discordErr.Message != "" {
			return fmt.Errorf("discord request failed with status %d: %s", resp.StatusCode, discordErr.Message)
		}
		return fmt.Errorf("discord request failed with status %d", resp.StatusCode)
	}

	return json.NewDecoder(resp.Body).Decode(out)
}
