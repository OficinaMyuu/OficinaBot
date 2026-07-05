package bot

import (
	"context"
	"errors"
	"fmt"
	"log/slog"
	"strings"

	"github.com/bwmarrin/discordgo"

	"oficina-registrar/internal/registration"
	"oficina-registrar/internal/store"
)

type RegisterSaver interface {
	Save(context.Context, store.RegisterRecord) error
}

type RegisterCommand struct {
	config  Config
	discord *DiscordService
	store   RegisterSaver
	logger  *slog.Logger
}

func NewRegisterCommand(cfg Config, discord *DiscordService, store RegisterSaver, logger *slog.Logger) *RegisterCommand {
	return &RegisterCommand{
		config:  cfg,
		discord: discord,
		store:   store,
		logger:  logger,
	}
}

func (c *RegisterCommand) Execute(ctx context.Context, command *CommandContext) error {
	message := command.Message
	issuer := message.Member
	if issuer == nil {
		return nil
	}
	hydrateMemberUser(issuer, message.Author)

	allowed, err := c.discord.HasRoleOrPermission(command.Session, message.GuildID, issuer, registration.RoleRegistrar, discordgo.PermissionManageRoles)
	if err != nil {
		return fmt.Errorf("check register permission: %w", err)
	}
	if !allowed {
		return nil
	}

	c.discord.DeleteMessage(command.Session, message.ChannelID, message.ID)

	pattern := argAt(command.Args, 0)
	action, err := registration.ParseAction(pattern)
	if err != nil {
		c.discord.SendTemporaryMessage(command.Session, message.ChannelID, "A sintaxe do comando está incorreta: `"+pattern+"`")
		return nil
	}

	if !action.HasValidAge() {
		c.discord.SendTemporaryMessage(command.Session, message.ChannelID, "Idade inválida: `"+pattern+"`.")
		return nil
	}

	targetID := registration.Digits(argAt(command.Args, 1))
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
		c.discord.SendTemporaryMessage(command.Session, message.ChannelID, "Você não pode registrar um bot.")
		return nil
	}

	if target.User != nil && issuer.User != nil && target.User.ID == issuer.User.ID {
		c.discord.SendTemporaryMessage(command.Session, message.ChannelID, "Você não pode registrar você mesmo.")
		return nil
	}

	if c.discord.MemberHasRole(target, registration.RoleRegistered) {
		c.discord.SendTemporaryMessage(command.Session, message.ChannelID, "Este membro já está registrado.")
		return nil
	}

	if c.discord.MemberHasRole(target, registration.RoleVerifying) {
		c.discord.SendTemporaryMessage(command.Session, message.ChannelID, "O usuário ainda está em verificação.")
		return nil
	}

	rolesAdd := action.RolesToAdd()
	rolesRemove := action.RolesToRemove()
	if missing, err := c.discord.EnsureRolesExist(command.Session, message.GuildID, rolesAdd); err != nil {
		return fmt.Errorf("check registration roles: %w", err)
	} else if missing != "" {
		c.logger.Warn("could not resolve registration role", "role", missing)
		c.discord.SendTemporaryMessage(command.Session, message.ChannelID, "Cargo necessário não encontrado: `"+string(missing)+"`.")
		return nil
	}

	if err := c.discord.ModifyMemberRoles(command.Session, message.GuildID, target, rolesAdd, rolesRemove); err != nil {
		c.discord.SendTemporaryMessage(command.Session, message.ChannelID, "Erro :/")
		return fmt.Errorf("add registration roles to target %s: %w", targetID, err)
	}

	targetInt, err := registration.SnowflakeInt64(targetID)
	if err != nil {
		return err
	}
	issuerID, err := memberIDInt64(issuer)
	if err != nil {
		return err
	}

	if err := c.store.Save(ctx, store.RegisterRecord{
		TargetID:    targetInt,
		ModeratorID: issuerID,
		Age:         action.Age,
		Gender:      action.Gender,
		Device:      action.Device,
	}); err != nil {
		c.discord.SendTemporaryMessage(command.Session, message.ChannelID, "Não foi possível salvar o registro no banco de dados.")
		return err
	}

	c.logRegistration(command.Session, message.GuildID, target, issuer, rolesAdd)
	c.discord.DeleteRecentMessagesByAuthor(command.Session, message.ChannelID, targetID, 20)
	c.discord.SendTemporaryMessage(command.Session, message.ChannelID, target.Mention()+" registrado com sucesso!")

	issuerName := userName(issuer)
	targetName := userName(target)
	c.logger.Info("member registered", "issuer", issuerName, "target", targetName, "target_id", targetID)
	return nil
}

func (c *RegisterCommand) logRegistration(session *discordgo.Session, guildID string, target *discordgo.Member, moderator *discordgo.Member, roles []registration.Role) {
	guild, err := session.Guild(guildID)
	if err != nil {
		c.logger.Warn("could not fetch guild for register log", "guild_id", guildID, "error", err)
	}

	roleMentions := make([]string, 0, len(roles))
	for _, role := range roles {
		roleMentions = append(roleMentions, role.Mention())
	}

	guildName := guildID
	guildIcon := ""
	if guild != nil {
		guildName = guild.Name
		guildIcon = guild.IconURL("")
	}

	embed := &discordgo.MessageEmbed{
		Color:       0x00ff00,
		Title:       "`" + userName(target) + "` foi registrado!",
		Description: "Registrado por `" + userName(moderator) + "`.",
		Thumbnail: &discordgo.MessageEmbedThumbnail{
			URL: userAvatarURL(target),
		},
		Fields: []*discordgo.MessageEmbedField{
			{Name: "Cargos", Value: strings.Join(roleMentions, "\n")},
		},
		Footer: &discordgo.MessageEmbedFooter{
			Text:    guildName + "・ID: " + userID(target),
			IconURL: guildIcon,
		},
	}

	if _, err := session.ChannelMessageSendEmbed(c.config.RegisterLogChannelID, embed); err != nil {
		c.logger.Warn("could not send register log embed", "channel_id", c.config.RegisterLogChannelID, "error", err)
	}
}

func argAt(args []string, index int) string {
	if index < 0 || index >= len(args) {
		return ""
	}
	return args[index]
}

func memberIDInt64(member *discordgo.Member) (int64, error) {
	if member == nil || member.User == nil {
		return 0, errors.New("member has no user")
	}
	return registration.SnowflakeInt64(member.User.ID)
}

func hydrateMemberUser(member *discordgo.Member, user *discordgo.User) {
	if member != nil && member.User == nil {
		member.User = user
	}
}

func userID(member *discordgo.Member) string {
	if member == nil || member.User == nil {
		return ""
	}
	return member.User.ID
}

func userName(member *discordgo.Member) string {
	if member == nil || member.User == nil {
		return ""
	}
	return member.User.Username
}

func userAvatarURL(member *discordgo.Member) string {
	if member == nil || member.User == nil {
		return ""
	}
	return member.User.AvatarURL("")
}
