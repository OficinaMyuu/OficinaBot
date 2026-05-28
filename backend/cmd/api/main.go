package main

import (
	"context"
	"oficina-img/internal/app"
	"os"
	"os/signal"
	"syscall"
)

func main() {
	ctx, stop := signal.NotifyContext(context.Background(), os.Interrupt, syscall.SIGTERM)
	defer stop()

	cfg, err := app.LoadConfig()
	if err != nil {
		panic(err)
	}

	server, err := app.NewServer()
	if err != nil {
		panic(err)
	}
	defer server.Close()

	if err := server.Start(ctx, cfg.Address); err != nil {
		panic(err)
	}
}
