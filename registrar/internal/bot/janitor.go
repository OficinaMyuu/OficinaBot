package bot

import (
	"context"
	"fmt"
	"log/slog"

	"github.com/bwmarrin/discordgo"

	"oficina-registrar/internal/registration"
)

type RegistryJanitor struct {
	config  Config
	discord *DiscordService
	logger  *slog.Logger
}

func NewRegistryJanitor(cfg Config, discord *DiscordService, logger *slog.Logger) *RegistryJanitor {
	return &RegistryJanitor{
		config:  cfg,
		discord: discord,
		logger:  logger,
	}
}

func (j *RegistryJanitor) HandleMessageCreate(ctx context.Context, session *discordgo.Session, event *discordgo.MessageCreate) {
	if event.ChannelID != j.config.RegistryChannelID {
		return
	}

	go func() {
		if err := j.deleteNumberlessMessage(ctx, session, event); err != nil {
			j.logger.Warn("registry message janitor failed", "error", err)
		}
	}()
}

func (j *RegistryJanitor) HandleGuildMemberRemove(ctx context.Context, session *discordgo.Session, event *discordgo.GuildMemberRemove) {
	go func() {
		if err := ctx.Err(); err != nil {
			return
		}
		j.discord.DeleteRecentMessagesByAuthor(session, j.config.RegistryChannelID, event.User.ID, 50)
	}()
}

func (j *RegistryJanitor) deleteNumberlessMessage(ctx context.Context, session *discordgo.Session, event *discordgo.MessageCreate) error {
	if err := ctx.Err(); err != nil {
		return nil
	}

	member := event.Member
	if member == nil {
		return nil
	}

	allowed, err := j.discord.HasRoleOrPermission(session, event.GuildID, member, registration.RoleRegistrar, discordgo.PermissionManageChannels)
	if err != nil {
		return fmt.Errorf("check registry janitor permission: %w", err)
	}
	if allowed {
		return nil
	}

	if registration.Digits(event.Content) == "" {
		j.discord.DeleteMessage(session, event.ChannelID, event.ID)
	}
	return nil
}
