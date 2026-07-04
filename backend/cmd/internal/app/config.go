package app

import (
	"fmt"
	"os"

	"github.com/joho/godotenv"
)

const DefaultAddress = ":8080"
const DefaultBodyLimit = "8M"

type Config struct {
	Address   string
	BodyLimit string
}

func LoadConfig() (Config, error) {
	// Load dotenv if it exists, but ignore errors if it is missing (as env vars are already injected in Docker/Prod)
	_ = godotenv.Load()

	return Config{
		Address:   getEnv("ADDRESS", DefaultAddress),
		BodyLimit: getEnv("BODY_LIMIT", DefaultBodyLimit),
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

func getEnv(key, fallback string) string {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}
	return value
}
