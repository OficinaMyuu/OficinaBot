package app

import (
	"context"
	"fmt"
	"log/slog"
	"time"

	"github.com/bwmarrin/discordgo"

	"oficina-registrar/internal/bot"
	"oficina-registrar/internal/config"
	"oficina-registrar/internal/database"
	"oficina-registrar/internal/store"
)

const (
	appTokenKey           = "registrar.token"
	registryChannelKey    = "channels.registry"
	registerLogChannelKey = "channels.registry.log"
)

func Run(ctx context.Context, logger *slog.Logger) error {
	dbSettings, err := config.LoadDatabaseSettings()
	if err != nil {
		return fmt.Errorf("load database settings: %w", err)
	}

	db, err := database.Open(ctx, dbSettings)
	if err != nil {
		return fmt.Errorf("open database: %w", err)
	}
	defer db.Close()

	configStore := store.NewConfigStore(db)
	token, err := configStore.Get(ctx, appTokenKey)
	if err != nil {
		return fmt.Errorf("load %s: %w", appTokenKey, err)
	}
	registryChannelID, err := configStore.Get(ctx, registryChannelKey)
	if err != nil {
		return fmt.Errorf("load %s: %w", registryChannelKey, err)
	}
	registerLogChannelID, err := configStore.Get(ctx, registerLogChannelKey)
	if err != nil {
		return fmt.Errorf("load %s: %w", registerLogChannelKey, err)
	}

	session, err := discordgo.New("Bot " + token)
	if err != nil {
		return fmt.Errorf("create Discord session: %w", err)
	}
	session.StateEnabled = false
	session.SyncEvents = false
	session.ShouldReconnectOnError = true
	session.ShouldRetryOnRateLimit = true
	session.Identify.Intents = discordgo.IntentGuilds |
		discordgo.IntentGuildMessages |
		discordgo.IntentGuildMembers |
		discordgo.IntentMessageContent

	registers := store.NewRegisterRepository(db, time.Now)
	registrar := bot.New(ctx, session, registers, bot.Config{
		Prefix:               "r!",
		RegistryChannelID:    registryChannelID,
		RegisterLogChannelID: registerLogChannelID,
		TemporaryMessageTTL:  10 * time.Second,
		RoleCacheTTL:         5 * time.Minute,
	}, logger)
	registrar.RegisterHandlers()

	logger.Info("opening Discord gateway")
	if err := session.Open(); err != nil {
		return fmt.Errorf("open Discord session: %w", err)
	}
	defer session.Close()

	if err := session.UpdateGameStatus(0, "Registro"); err != nil {
		logger.Warn("could not update Discord presence", "error", err)
	}

	logger.Info("registrar is ready")
	<-ctx.Done()
	logger.Info("shutdown signal received")
	return nil
}
