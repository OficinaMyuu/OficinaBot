package auth

import (
	"context"
	"errors"
	"strings"
	"time"

	"gorm.io/gorm"
	"oficina-img/internal/repository"
)

const bearerPrefix = "Bearer "

var ErrInvalidServiceToken = errors.New("invalid service token")

type BotClientStore interface {
	GetByTokenHash(ctx context.Context, tokenHash string) (*repository.BotClient, error)
	Touch(ctx context.Context, name string, seenAt time.Time) error
}

type ServiceAuthenticator struct {
	clients BotClientStore
	now     func() time.Time
}

func NewServiceAuthenticator(clients BotClientStore) *ServiceAuthenticator {
	return &ServiceAuthenticator{
		clients: clients,
		now:     func() time.Time { return time.Now().UTC() },
	}
}

func (a *ServiceAuthenticator) Authenticate(ctx context.Context, authorization string) (*repository.BotClient, error) {
	token, ok := parseBearerToken(authorization)
	if !ok {
		return nil, ErrInvalidServiceToken
	}

	client, err := a.clients.GetByTokenHash(ctx, ServiceTokenHash(token))
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, ErrInvalidServiceToken
		}
		return nil, err
	}
	if err := a.clients.Touch(ctx, client.Name, a.now()); err != nil {
		return nil, err
	}
	return client, nil
}

func ServiceTokenHash(token string) string {
	return HashToken(token)
}

func parseBearerToken(authorization string) (string, bool) {
	if !strings.HasPrefix(authorization, bearerPrefix) {
		return "", false
	}
	token := strings.TrimSpace(strings.TrimPrefix(authorization, bearerPrefix))
	if token == "" {
		return "", false
	}
	return token, true
}
