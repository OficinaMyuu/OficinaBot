package app

import (
	"fmt"
	"os"
	"strconv"
	"time"

	"github.com/joho/godotenv"
)

const DefaultAddress = ":8080"
const DefaultDatabasePath = "./data/oficina-services.db"
const DefaultFrontendRedirectURL = "http://localhost:5173"
const DefaultFrontendOrigin = "http://localhost:5173"
const DefaultBodyLimit = "8M"
const DefaultSessionTTL = 7 * 24 * time.Hour
const DefaultSessionCookieName = "oficina_admin_session"

type Config struct {
	Address             string
	DatabasePath        string
	DiscordClientID     string
	DiscordClientSecret string
	DiscordRedirectURL  string
	FrontendRedirectURL string
	FrontendOrigin      string
	BodyLimit           string
	OwnerDiscordID      string
	SessionSecret       string
	SessionTTL          time.Duration
	SessionCookieName   string
	SessionCookieSecure bool
}

func LoadConfig() (Config, error) {
	if err := godotenv.Load(); err != nil {
		return Config{}, err
	}

	return Config{
		Address:             getEnv("ADDRESS", DefaultAddress),
		DatabasePath:        getEnv("DATABASE_PATH", DefaultDatabasePath),
		DiscordClientID:     os.Getenv("DISCORD_CLIENT_ID"),
		DiscordClientSecret: os.Getenv("DISCORD_CLIENT_SECRET"),
		DiscordRedirectURL:  os.Getenv("DISCORD_REDIRECT_URL"),
		FrontendRedirectURL: getEnv("FRONTEND_REDIRECT_URL", DefaultFrontendRedirectURL),
		FrontendOrigin:      getEnv("FRONTEND_ORIGIN", DefaultFrontendOrigin),
		BodyLimit:           getEnv("BODY_LIMIT", DefaultBodyLimit),
		OwnerDiscordID:      os.Getenv("OFICINA_OWNER_DISCORD_ID"),
		SessionSecret:       os.Getenv("SESSION_SECRET"),
		SessionTTL:          DefaultSessionTTL,
		SessionCookieName:   getEnv("SESSION_COOKIE_NAME", DefaultSessionCookieName),
		SessionCookieSecure: getBoolEnv("SESSION_COOKIE_SECURE", true),
	}, nil
}

func (c Config) ValidateAuth() error {
	missing := make([]string, 0)
	if c.DiscordClientID == "" {
		missing = append(missing, "DISCORD_CLIENT_ID")
	}
	if c.DiscordClientSecret == "" {
		missing = append(missing, "DISCORD_CLIENT_SECRET")
	}
	if c.DiscordRedirectURL == "" {
		missing = append(missing, "DISCORD_REDIRECT_URL")
	}
	if c.OwnerDiscordID == "" {
		missing = append(missing, "OFICINA_OWNER_DISCORD_ID")
	}
	if c.SessionSecret == "" {
		missing = append(missing, "SESSION_SECRET")
	}
	if len(missing) > 0 {
		return fmt.Errorf("missing auth config: %v", missing)
	}
	if c.SessionTTL <= 0 {
		return fmt.Errorf("session TTL must be positive")
	}
	return nil
}

func getEnv(key, fallback string) string {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}
	return value
}

func getBoolEnv(key string, fallback bool) bool {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}
	parsed, err := strconv.ParseBool(value)
	if err != nil {
		return fallback
	}
	return parsed
}
