package app

import (
	"os"

	"github.com/joho/godotenv"
)

const DefaultAddress = ":8080"
const DefaultDatabasePath = "./data/oficina-services.db"

type Config struct {
	Address      string
	DatabasePath string
}

func LoadConfig() (Config, error) {
	if err := godotenv.Load(); err != nil {
		return Config{}, err
	}

	return Config{
		Address:      getEnv("ADDRESS", DefaultAddress),
		DatabasePath: getEnv("DATABASE_PATH", DefaultDatabasePath),
	}, nil
}

func getEnv(key, fallback string) string {
	value := os.Getenv(key)
	if value == "" {
		return fallback
	}
	return value
}
