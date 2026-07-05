package bot

import (
	"context"
	"fmt"
	"log/slog"

	"github.com/bwmarrin/discordgo"

	"oficina-registrar/internal/registration"
)

type RevokeCommand struct {
	discord *DiscordService
	logger  *slog.Logger
}

func NewRevokeCommand(discord *DiscordService, logger *slog.Logger) *RevokeCommand {
	return &RevokeCommand{discord: discord, logger: logger}
}

func (c *RevokeCommand) Execute(_ context.Context, command *CommandContext) error {
	message := command.Message
	issuer := message.Member
	if issuer == nil {
		return nil
	}
	hydrateMemberUser(issuer, message.Author)

	allowed, err := c.discord.HasGuildPermission(command.Session, message.GuildID, issuer, discordgo.PermissionManageGuild)
	if err != nil {
		return fmt.Errorf("check revoke permission: %w", err)
	}
	if !allowed {
		return nil
	}

	c.discord.DeleteMessage(command.Session, message.ChannelID, message.ID)

	targetID := registration.Digits(argAt(command.Args, 0))
	if targetID == "" {
		c.discord.SendTemporaryMessage(command.Session, message.ChannelID, "Membro não encontrado.")
		return nil
	}

	target, err := c.discord.FetchMember(command.Session, message.GuildID, targetID)
	if err != nil {
		c.discord.SendTemporaryMessage(command.Session, message.ChannelID, "Membro não encontrado.")
		return nil
	}

	if target.User != nil && target.User.Bot {
		c.discord.SendTemporaryMessage(command.Session, message.ChannelID, "Você não pode remover o registro de um bot.")
		return nil
	}

	if target.User != nil && issuer.User != nil && target.User.ID == issuer.User.ID {
		c.discord.SendTemporaryMessage(command.Session, message.ChannelID, "Você não pode remover o seu próprio registro.")
		return nil
	}

	if !c.discord.MemberHasRole(target, registration.RoleRegistered) {
		c.discord.SendTemporaryMessage(command.Session, message.ChannelID, "Este membro não está registrado.")
		return nil
	}

	if c.discord.MemberHasRole(target, registration.RoleVerifying) {
		c.discord.SendTemporaryMessage(command.Session, message.ChannelID, "O usuário ainda está em verificação.")
		return nil
	}

	if err := c.discord.ModifyMemberRoles(command.Session, message.GuildID, target, registration.NonRegisteredRoles(), registration.RegisteredRoles()); err != nil {
		c.discord.SendTemporaryMessage(command.Session, message.ChannelID, "Erro :/")
		return fmt.Errorf("revoke registration roles from target %s: %w", targetID, err)
	}

	c.discord.SendTemporaryMessage(command.Session, message.ChannelID, "Registro de "+target.Mention()+" removido com sucesso!")
	c.logger.Info("member registration revoked", "issuer", userName(issuer), "target", userName(target), "target_id", targetID)
	return nil
}
