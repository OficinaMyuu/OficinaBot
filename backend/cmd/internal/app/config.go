package app

import "github.com/joho/godotenv"

const DefaultAddress = ":8080"

type Config struct {
	Address string
}

func LoadConfig() (Config, error) {
	if err := godotenv.Load(); err != nil {
		return Config{}, err
	}

	return Config{
		Address: DefaultAddress,
	}, nil
}
