package main

import (
	"oficina-img/internal/app"
)

func main() {
	cfg, err := app.LoadConfig()
	if err != nil {
		panic(err)
	}

	server, err := app.NewServer()
	if err != nil {
		panic(err)
	}
	defer server.Close()

	if err := server.Echo.Start(cfg.Address); err != nil {
		server.Echo.Logger.Fatal(err)
	}
}
