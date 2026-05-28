package main

import (
	"oficina-img/internal/app"
)

func main() {
	cfg, err := app.LoadConfig()
	if err != nil {
		panic(err)
	}

	e, err := app.NewServer()
	if err != nil {
		panic(err)
	}

	if err := e.Start(cfg.Address); err != nil {
		e.Logger.Fatal(err)
	}
}
