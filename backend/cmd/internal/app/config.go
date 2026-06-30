package app

import (
	"fmt"
	"net"
	"os"
	"strconv"
	"time"

	mysqldriver "github.com/go-sql-driver/mysql"
	"github.com/joho/godotenv"
	"oficina-img/internal/database"
)

const DefaultAddress = ":8080"
const DefaultDatabaseName = "oficina_services"
const DefaultDatabasePort = "3306"
const DefaultFrontendRedirectURL = "http://localhost:5173"
const DefaultBodyLimit = "8M"
const DefaultSessionTTL = 7 * 24 * time.Hour
const DefaultSessionCookieName = "oficina_admin_session"

type Config struct {
	Address             string
	DatabaseHost        string
	DatabasePort        string
	DatabaseName        string
	DatabaseUser        string
	DatabasePassword    string
	DiscordClientID     string
	DiscordClientSecret string
	DiscordRedirectURL  string
	DiscordBotToken     string
	FrontendRedirectURL string
	BodyLimit           string
	OwnerDiscordID      string
	SessionSecret       string
	SessionTTL          time.Duration
	SessionCookieName   string
	SessionCookieSecure bool
}

func LoadConfig() (Config, error) {
	// Load dotenv if it exists, but ignore errors if it is missing (as env vars are already injected in Docker/Prod)
	_ = godotenv.Load()

	return Config{
		Address:             getEnv("ADDRESS", DefaultAddress),
		DatabaseHost:        os.Getenv("DATABASE_HOST"),
		DatabasePort:        getEnv("DATABASE_PORT", DefaultDatabasePort),
		DatabaseName:        getEnv("DATABASE_NAME", DefaultDatabaseName),
		DatabaseUser:        os.Getenv("DATABASE_USER"),
		DatabasePassword:    os.Getenv("DATABASE_PASSWORD"),
		DiscordClientID:     os.Getenv("DISCORD_CLIENT_ID"),
		DiscordClientSecret: os.Getenv("DISCORD_CLIENT_SECRET"),
		DiscordRedirectURL:  os.Getenv("DISCORD_REDIRECT_URL"),
		DiscordBotToken:     os.Getenv("DISCORD_BOT_TOKEN"),
		FrontendRedirectURL: getEnv("FRONTEND_REDIRECT_URL", DefaultFrontendRedirectURL),
		BodyLimit:           getEnv("BODY_LIMIT", DefaultBodyLimit),
		OwnerDiscordID:      os.Getenv("OFICINA_OWNER_DISCORD_ID"),
		SessionSecret:       os.Getenv("SESSION_SECRET"),
		SessionTTL:          DefaultSessionTTL,
		SessionCookieName:   getEnv("SESSION_COOKIE_NAME", DefaultSessionCookieName),
		SessionCookieSecure: getBoolEnv("SESSION_COOKIE_SECURE", true),
	}, nil
}

func (c Config) ValidateRuntime() error {
	if _, err := c.DatabaseConfig(); err != nil {
		return err
	}
	missing := make([]string, 0)
	if c.DiscordBotToken == "" {
		missing = append(missing, "DISCORD_BOT_TOKEN")
	}
	if c.SessionSecret == "" {
		missing = append(missing, "SESSION_SECRET")
	}
	if len(missing) > 0 {
		return fmt.Errorf("missing runtime config: %v", missing)
	}
	if err := c.validateDiscordOAuthConfig(); err != nil {
		return err
	}
	if c.SessionTTL <= 0 {
		return fmt.Errorf("session TTL must be positive")
	}
	return nil
}

func (c Config) DatabaseConfig() (database.Config, error) {
	missing := make([]string, 0)
	if c.DatabaseHost == "" {
		missing = append(missing, "DATABASE_HOST")
	}
	if c.DatabasePort == "" {
		missing = append(missing, "DATABASE_PORT")
	}
	if c.DatabaseName == "" {
		missing = append(missing, "DATABASE_NAME")
	}
	if c.DatabaseUser == "" {
		missing = append(missing, "DATABASE_USER")
	}
	if c.DatabasePassword == "" {
		missing = append(missing, "DATABASE_PASSWORD")
	}
	if len(missing) > 0 {
		return database.Config{}, fmt.Errorf("missing database config: %v", missing)
	}

	mysqlConfig := mysqldriver.NewConfig()
	mysqlConfig.User = c.DatabaseUser
	mysqlConfig.Passwd = c.DatabasePassword
	mysqlConfig.Net = "tcp"
	mysqlConfig.Addr = net.JoinHostPort(c.DatabaseHost, c.DatabasePort)
	mysqlConfig.DBName = c.DatabaseName
	mysqlConfig.ParseTime = true
	mysqlConfig.Loc = time.UTC
	mysqlConfig.Params = map[string]string{
		"charset":   "utf8mb4",
		"collation": "utf8mb4_unicode_ci",
	}

	return database.Config{DSN: mysqlConfig.FormatDSN()}, nil
}

func (c Config) validateDiscordOAuthConfig() error {
	oauthValues := []string{
		c.DiscordClientID,
		c.DiscordClientSecret,
		c.DiscordRedirectURL,
		c.OwnerDiscordID,
	}
	configured := false
	for _, value := range oauthValues {
		if value != "" {
			configured = true
			break
		}
	}
	if !configured {
		return nil
	}

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
	if len(missing) > 0 {
		return fmt.Errorf("incomplete Discord OAuth config: %v", missing)
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
