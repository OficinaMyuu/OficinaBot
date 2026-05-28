package auth

import (
	"context"
	"errors"
	"testing"
	"time"

	"gorm.io/gorm"
	"oficina-img/internal/repository"
)

func TestServiceAuthenticatorAuthenticatesBearerToken(t *testing.T) {
	store := &fakeBotClientStore{
		clientsByHash: map[string]*repository.BotClient{
			ServiceTokenHash("secret"): {Name: "bot", TokenHash: ServiceTokenHash("secret")},
		},
	}
	authenticator := NewServiceAuthenticator(store)
	authenticator.now = func() time.Time { return nowForServiceAuthTest }

	client, err := authenticator.Authenticate(context.Background(), "Bearer secret")

	if err != nil {
		t.Fatalf("authenticate service token: %v", err)
	}
	if client.Name != "bot" {
		t.Fatalf("expected bot client, got %+v", client)
	}
	if store.touchedName != "bot" || !store.touchedAt.Equal(nowForServiceAuthTest) {
		t.Fatalf("expected bot touch at %v, got %q %v", nowForServiceAuthTest, store.touchedName, store.touchedAt)
	}
}

func TestServiceAuthenticatorRejectsMissingBearerToken(t *testing.T) {
	authenticator := NewServiceAuthenticator(&fakeBotClientStore{})

	if _, err := authenticator.Authenticate(context.Background(), "Basic secret"); !errors.Is(err, ErrInvalidServiceToken) {
		t.Fatalf("expected invalid service token, got %v", err)
	}
}

func TestServiceAuthenticatorRejectsUnknownToken(t *testing.T) {
	authenticator := NewServiceAuthenticator(&fakeBotClientStore{clientsByHash: map[string]*repository.BotClient{}})

	if _, err := authenticator.Authenticate(context.Background(), "Bearer secret"); !errors.Is(err, ErrInvalidServiceToken) {
		t.Fatalf("expected invalid service token, got %v", err)
	}
}

var nowForServiceAuthTest = time.Unix(200, 0).UTC()

type fakeBotClientStore struct {
	clientsByHash map[string]*repository.BotClient
	touchedName   string
	touchedAt     time.Time
}

func (s *fakeBotClientStore) GetByTokenHash(_ context.Context, tokenHash string) (*repository.BotClient, error) {
	client, ok := s.clientsByHash[tokenHash]
	if !ok {
		return nil, gorm.ErrRecordNotFound
	}
	copy := *client
	return &copy, nil
}

func (s *fakeBotClientStore) Touch(_ context.Context, name string, seenAt time.Time) error {
	s.touchedName = name
	s.touchedAt = seenAt
	return nil
}
