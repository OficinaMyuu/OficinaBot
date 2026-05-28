package app

import "github.com/joho/godotenv"

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
		Address:      DefaultAddress,
		DatabasePath: DefaultDatabasePath,
	}, nil
}
