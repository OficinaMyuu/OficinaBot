package app

import (
	"fmt"
	"net"
	"net/url"
	"os"
	"strings"

	"github.com/go-sql-driver/mysql"
	"github.com/joho/godotenv"
)

const DefaultAddress = ":8080"
const DefaultBodyLimit = "8M"
const DefaultPublicAPIBaseURL = "http://localhost:8080"
const DefaultFrontendBaseURL = "http://localhost:5173"
const DefaultDiscordAPIBaseURL = "https://discord.com/api/v10"
const DefaultDiscordAuthorizeURL = "https://discord.com/oauth2/authorize"

type Config struct {
	Address   string
	BodyLimit string
	Database  DatabaseConfig
	Dashboard DashboardConfig
}

type DatabaseConfig struct {
	DSN      string
	Host     string
	Port     string
	Name     string
	User     string
	Password string
}

type DashboardConfig struct {
	PublicAPIBaseURL    string
	FrontendBaseURL     string
	CORSAllowedOrigins  []string
	DiscordAPIBaseURL   string
	DiscordAuthorizeURL string
	DiscordClientID     string
	DiscordClientSecret string
	DiscordGuildID      string
	DiscordBotToken     string
}

func LoadConfig() (Config, error) {
	// Load dotenv if it exists, but ignore errors if it is missing (as env vars are already injected in Docker/Prod)
	_ = godotenv.Load()

	return Config{
		Address:   getEnv("ADDRESS", DefaultAddress),
		BodyLimit: getEnv("BODY_LIMIT", DefaultBodyLimit),
		Database: DatabaseConfig{
			DSN:      os.Getenv("DATABASE_DSN"),
			Host:     os.Getenv("DATABASE_HOST"),
			Port:     getEnv("DATABASE_PORT", "3306"),
			Name:     os.Getenv("DATABASE_NAME"),
			User:     os.Getenv("DATABASE_USER"),
			Password: os.Getenv("DATABASE_PASSWORD"),
		},
		Dashboard: DashboardConfig{
			PublicAPIBaseURL:    trimTrailingSlash(getEnv("PUBLIC_API_BASE_URL", DefaultPublicAPIBaseURL)),
			FrontendBaseURL:     trimTrailingSlash(getEnv("FRONTEND_BASE_URL", DefaultFrontendBaseURL)),
			CORSAllowedOrigins:  parseOrigins(os.Getenv("CORS_ALLOWED_ORIGINS")),
			DiscordAPIBaseURL:   strings.TrimRight(getEnv("DISCORD_API_BASE_URL", DefaultDiscordAPIBaseURL), "/"),
			DiscordAuthorizeURL: getEnv("DISCORD_AUTHORIZE_URL", DefaultDiscordAuthorizeURL),
			DiscordClientID:     os.Getenv("DISCORD_CLIENT_ID"),
			DiscordClientSecret: os.Getenv("DISCORD_CLIENT_SECRET"),
			DiscordGuildID:      os.Getenv("DISCORD_GUILD_ID"),
			DiscordBotToken:     os.Getenv("DISCORD_BOT_TOKEN"),
		},
	}, nil
}

func (c Config) ValidateRuntime() error {
	if c.Address == "" {
		return fmt.Errorf("address must not be empty")
	}
	if c.BodyLimit == "" {
		return fmt.Errorf("body limit must not be empty")
	}
	return nil
}

func (c Config) MissingDashboardConfig() []string {
	missing := c.Dashboard.MissingConfig()
	missing = append(missing, c.Database.MissingConfig()...)
	return missing
}

func (c Config) DashboardReady() bool {
	return len(c.MissingDashboardConfig()) == 0
}

func (c DatabaseConfig) MissingConfig() []string {
	if c.DSN != "" {
		return nil
	}

	var missing []string
	if c.Host == "" {
		missing = append(missing, "DATABASE_HOST")
	}
	if c.Port == "" {
		missing = append(missing, "DATABASE_PORT")
	}
	if c.Name == "" {
		missing = append(missing, "DATABASE_NAME")
	}
	if c.User == "" {
		missing = append(missing, "DATABASE_USER")
	}
	if c.Password == "" {
		missing = append(missing, "DATABASE_PASSWORD")
	}
	return missing
}

func (c DatabaseConfig) FormatDSN() (string, error) {
	if c.DSN != "" {
		cfg, err := mysql.ParseDSN(c.DSN)
		if err != nil {
			return "", err
		}
		cfg.ParseTime = true
		return cfg.FormatDSN(), nil
	}

	if missing := c.MissingConfig(); len(missing) > 0 {
		return "", fmt.Errorf("missing database config: %s", strings.Join(missing, ", "))
	}

	cfg := mysql.Config{
		User:      c.User,
		Passwd:    c.Password,
		Net:       "tcp",
		Addr:      net.JoinHostPort(c.Host, c.Port),
		DBName:    c.Name,
		ParseTime: true,
		Params: map[string]string{
			"charset":   "utf8mb4",
			"collation": "utf8mb4_unicode_ci",
		},
	}
	return cfg.FormatDSN(), nil
}

func (c DashboardConfig) MissingConfig() []string {
	var missing []string
	if c.PublicAPIBaseURL == "" {
		missing = append(missing, "PUBLIC_API_BASE_URL")
	}
	if c.FrontendBaseURL == "" {
		missing = append(missing, "FRONTEND_BASE_URL")
	}
	if c.DiscordAPIBaseURL == "" {
		missing = append(missing, "DISCORD_API_BASE_URL")
	}
	if c.DiscordAuthorizeURL == "" {
		missing = append(missing, "DISCORD_AUTHORIZE_URL")
	}
	if c.DiscordClientID == "" {
		missing = append(missing, "DISCORD_CLIENT_ID")
	}
	if c.DiscordClientSecret == "" {
		missing = append(missing, "DISCORD_CLIENT_SECRET")
	}
	if c.DiscordGuildID == "" {
		missing = append(missing, "DISCORD_GUILD_ID")
	}
	return missing
}

func (c DashboardConfig) CookieSecure() bool {
	return strings.HasPrefix(strings.ToLower(c.PublicAPIBaseURL), "https://")
}

func (c DashboardConfig) AllowedCORSOrigins() []string {
	if len(c.CORSAllowedOrigins) > 0 {
		return c.CORSAllowedOrigins
	}
	origin, ok := originFromURL(c.FrontendBaseURL)
	if !ok {
		return nil
	}
	return []string{origin}
}

func getEnv(key, fallback string) string {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}
	return value
}

func trimTrailingSlash(value string) string {
	return strings.TrimRight(value, "/")
}

func parseOrigins(raw string) []string {
	if raw == "" {
		return nil
	}

	fields := strings.FieldsFunc(raw, func(r rune) bool {
		return r == ',' || r == ';' || r == '\n' || r == '\r' || r == '\t' || r == ' '
	})
	origins := make([]string, 0, len(fields))
	seen := make(map[string]struct{}, len(fields))
	for _, field := range fields {
		origin, ok := originFromURL(field)
		if !ok {
			continue
		}
		if _, exists := seen[origin]; exists {
			continue
		}
		seen[origin] = struct{}{}
		origins = append(origins, origin)
	}
	return origins
}

func originFromURL(raw string) (string, bool) {
	value := strings.TrimSpace(raw)
	if value == "" {
		return "", false
	}
	parsed, err := url.Parse(value)
	if err != nil || parsed.Scheme == "" || parsed.Host == "" {
		return "", false
	}
	scheme := strings.ToLower(parsed.Scheme)
	if scheme != "http" && scheme != "https" {
		return "", false
	}
	return scheme + "://" + parsed.Host, true
}
