package discord

import (
	"context"
	"testing"

	"github.com/bwmarrin/discordgo"
)

func TestMetadataServiceCachesGuild(t *testing.T) {
	client := &fakeRESTClient{guild: &discordgo.Guild{ID: "guild", Name: "Oficina"}}
	service := NewMetadataService(client)

	first, err := service.Guild(context.Background(), "guild")
	if err != nil {
		t.Fatalf("first guild lookup: %v", err)
	}
	second, err := service.Guild(context.Background(), "guild")
	if err != nil {
		t.Fatalf("second guild lookup: %v", err)
	}

	if first.Name != "Oficina" || second.Name != "Oficina" {
		t.Fatalf("expected Oficina guilds, got %+v %+v", first, second)
	}
	if client.guildCalls != 1 {
		t.Fatalf("expected one REST guild call, got %d", client.guildCalls)
	}
}

func TestMetadataServiceReturnsChannelRolesAndUser(t *testing.T) {
	client := &fakeRESTClient{
		channel: &discordgo.Channel{ID: "channel", GuildID: "guild", Name: "general", Type: discordgo.ChannelTypeGuildText},
		roles:   []*discordgo.Role{{ID: "role", Name: "Admin", Color: 123, Position: 1}},
		user:    &discordgo.User{ID: "user", Username: "myuu", GlobalName: "Myuu"},
	}
	service := NewMetadataService(client)

	channel, err := service.Channel(context.Background(), "channel")
	if err != nil {
		t.Fatalf("channel lookup: %v", err)
	}
	roles, err := service.GuildRoles(context.Background(), "guild")
	if err != nil {
		t.Fatalf("roles lookup: %v", err)
	}
	user, err := service.User(context.Background(), "user")
	if err != nil {
		t.Fatalf("user lookup: %v", err)
	}

	if channel.Name != "general" {
		t.Fatalf("expected general channel, got %+v", channel)
	}
	if len(roles) != 1 || roles[0].Name != "Admin" {
		t.Fatalf("expected Admin role, got %+v", roles)
	}
	if user.GlobalName != "Myuu" {
		t.Fatalf("expected Myuu user, got %+v", user)
	}
}

type fakeRESTClient struct {
	guild        *discordgo.Guild
	channel      *discordgo.Channel
	roles        []*discordgo.Role
	user         *discordgo.User
	guildCalls   int
	channelCalls int
	rolesCalls   int
	userCalls    int
}

func (f *fakeRESTClient) Guild(_ context.Context, _ string) (*discordgo.Guild, error) {
	f.guildCalls++
	return f.guild, nil
}

func (f *fakeRESTClient) Channel(_ context.Context, _ string) (*discordgo.Channel, error) {
	f.channelCalls++
	return f.channel, nil
}

func (f *fakeRESTClient) GuildRoles(_ context.Context, _ string) ([]*discordgo.Role, error) {
	f.rolesCalls++
	return f.roles, nil
}

func (f *fakeRESTClient) User(_ context.Context, _ string) (*discordgo.User, error) {
	f.userCalls++
	return f.user, nil
}
