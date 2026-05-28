package discord

import (
	"strings"
	"testing"
)

func TestNewClientRequiresValidBotToken(t *testing.T) {
	client, err := NewClient("token")
	if err != nil {
		t.Fatalf("new discord client: %v", err)
	}
	if client.session == nil {
		t.Fatal("expected discord session")
	}
}

func TestNewClientUsesBotTokenPrefix(t *testing.T) {
	client, err := NewClient("token")
	if err != nil {
		t.Fatalf("new discord client: %v", err)
	}
	if !strings.HasPrefix(client.session.Token, "Bot ") {
		t.Fatalf("expected bot token prefix, got %q", client.session.Token)
	}
}
