package discord

import (
	"context"
	"fmt"

	"github.com/bwmarrin/discordgo"
)

type RESTClient interface {
	Guild(ctx context.Context, guildID string) (*discordgo.Guild, error)
	Channel(ctx context.Context, channelID string) (*discordgo.Channel, error)
	GuildRoles(ctx context.Context, guildID string) ([]*discordgo.Role, error)
	User(ctx context.Context, userID string) (*discordgo.User, error)
}

type Client struct {
	session *discordgo.Session
}

func NewClient(botToken string) (*Client, error) {
	session, err := discordgo.New("Bot " + botToken)
	if err != nil {
		return nil, fmt.Errorf("create discord session: %w", err)
	}
	return &Client{session: session}, nil
}

func (c *Client) Guild(_ context.Context, guildID string) (*discordgo.Guild, error) {
	return c.session.Guild(guildID)
}

func (c *Client) Channel(_ context.Context, channelID string) (*discordgo.Channel, error) {
	return c.session.Channel(channelID)
}

func (c *Client) GuildRoles(_ context.Context, guildID string) ([]*discordgo.Role, error) {
	return c.session.GuildRoles(guildID)
}

func (c *Client) User(_ context.Context, userID string) (*discordgo.User, error) {
	return c.session.User(userID)
}
