package bot

import (
	"context"
	"log/slog"
	"time"

	"github.com/bwmarrin/discordgo"

	"oficina-registrar/internal/store"
)

type Config struct {
	Prefix               string
	RegistryChannelID    string
	RegisterLogChannelID string
	TemporaryMessageTTL  time.Duration
	RoleCacheTTL         time.Duration
}

type Bot struct {
	ctx     context.Context
	session *discordgo.Session
	config  Config
	logger  *slog.Logger
	router  *Router
	janitor *RegistryJanitor
}

func New(ctx context.Context, session *discordgo.Session, registers *store.RegisterRepository, cfg Config, logger *slog.Logger) *Bot {
	discord := NewDiscordService(cfg.RoleCacheTTL, cfg.TemporaryMessageTTL, logger)
	register := NewRegisterCommand(cfg, discord, registers, logger)
	revoke := NewRevokeCommand(discord, logger)

	router := NewRouter(cfg.Prefix, logger)
	router.Register("revoke", revoke)
	router.SetFallback(register)

	return &Bot{
		ctx:     ctx,
		session: session,
		config:  cfg,
		logger:  logger,
		router:  router,
		janitor: NewRegistryJanitor(cfg, discord, logger),
	}
}

func (b *Bot) RegisterHandlers() {
	b.session.AddHandler(b.onReady)
	b.session.AddHandler(b.onMessageCreate)
	b.session.AddHandler(b.onGuildMemberRemove)
}

func (b *Bot) onReady(_ *discordgo.Session, event *discordgo.Ready) {
	if event == nil || event.User == nil {
		b.logger.Info("discord ready")
		return
	}
	b.logger.Info("discord ready", "user_id", event.User.ID, "username", event.User.Username)
}

func (b *Bot) onMessageCreate(session *discordgo.Session, event *discordgo.MessageCreate) {
	if event == nil || event.GuildID == "" {
		return
	}
	if b.router.Dispatch(b.ctx, session, event) {
		return
	}
	b.janitor.HandleMessageCreate(b.ctx, session, event)
}

func (b *Bot) onGuildMemberRemove(session *discordgo.Session, event *discordgo.GuildMemberRemove) {
	if event == nil || event.GuildID == "" || event.User == nil {
		return
	}
	b.janitor.HandleGuildMemberRemove(b.ctx, session, event)
}
