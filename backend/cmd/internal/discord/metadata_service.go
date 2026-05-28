package discord

import (
	"context"

	"github.com/bwmarrin/discordgo"
)

type MetadataService struct {
	client   RESTClient
	guilds   *Cache[GuildMetadata]
	channels *Cache[ChannelMetadata]
	roles    *Cache[[]RoleMetadata]
	users    *Cache[UserMetadata]
}

func NewMetadataService(client RESTClient) *MetadataService {
	return &MetadataService{
		client:   client,
		guilds:   NewCache[GuildMetadata](DefaultCacheTTL),
		channels: NewCache[ChannelMetadata](DefaultCacheTTL),
		roles:    NewCache[[]RoleMetadata](DefaultCacheTTL),
		users:    NewCache[UserMetadata](DefaultCacheTTL),
	}
}

func (s *MetadataService) Guild(ctx context.Context, guildID string) (GuildMetadata, error) {
	if guild, ok := s.guilds.Get(guildID); ok {
		return guild, nil
	}

	guild, err := s.client.Guild(ctx, guildID)
	if err != nil {
		return GuildMetadata{}, err
	}
	metadata := guildMetadata(guild)
	s.guilds.Set(guildID, metadata)
	return metadata, nil
}

func (s *MetadataService) Channel(ctx context.Context, channelID string) (ChannelMetadata, error) {
	if channel, ok := s.channels.Get(channelID); ok {
		return channel, nil
	}

	channel, err := s.client.Channel(ctx, channelID)
	if err != nil {
		return ChannelMetadata{}, err
	}
	metadata := channelMetadata(channel)
	s.channels.Set(channelID, metadata)
	return metadata, nil
}

func (s *MetadataService) GuildRoles(ctx context.Context, guildID string) ([]RoleMetadata, error) {
	if roles, ok := s.roles.Get(guildID); ok {
		return roles, nil
	}

	roles, err := s.client.GuildRoles(ctx, guildID)
	if err != nil {
		return nil, err
	}
	metadata := make([]RoleMetadata, 0, len(roles))
	for _, role := range roles {
		metadata = append(metadata, roleMetadata(role))
	}
	s.roles.Set(guildID, metadata)
	return metadata, nil
}

func (s *MetadataService) User(ctx context.Context, userID string) (UserMetadata, error) {
	if user, ok := s.users.Get(userID); ok {
		return user, nil
	}

	user, err := s.client.User(ctx, userID)
	if err != nil {
		return UserMetadata{}, err
	}
	metadata := userMetadata(user)
	s.users.Set(userID, metadata)
	return metadata, nil
}

type GuildMetadata struct {
	ID      string `json:"id"`
	Name    string `json:"name"`
	IconURL string `json:"icon_url"`
}

type ChannelMetadata struct {
	ID      string `json:"id"`
	GuildID string `json:"guild_id"`
	Name    string `json:"name"`
	Type    int    `json:"type"`
}

type RoleMetadata struct {
	ID       string `json:"id"`
	Name     string `json:"name"`
	Color    int    `json:"color"`
	Position int    `json:"position"`
}

type UserMetadata struct {
	ID         string `json:"id"`
	Username   string `json:"username"`
	GlobalName string `json:"global_name"`
	AvatarURL  string `json:"avatar_url"`
}

func guildMetadata(guild *discordgo.Guild) GuildMetadata {
	return GuildMetadata{
		ID:      guild.ID,
		Name:    guild.Name,
		IconURL: guild.IconURL(""),
	}
}

func channelMetadata(channel *discordgo.Channel) ChannelMetadata {
	return ChannelMetadata{
		ID:      channel.ID,
		GuildID: channel.GuildID,
		Name:    channel.Name,
		Type:    int(channel.Type),
	}
}

func roleMetadata(role *discordgo.Role) RoleMetadata {
	return RoleMetadata{
		ID:       role.ID,
		Name:     role.Name,
		Color:    role.Color,
		Position: role.Position,
	}
}

func userMetadata(user *discordgo.User) UserMetadata {
	return UserMetadata{
		ID:         user.ID,
		Username:   user.Username,
		GlobalName: user.GlobalName,
		AvatarURL:  user.AvatarURL(""),
	}
}
