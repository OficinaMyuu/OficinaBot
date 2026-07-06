package store

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"sync"
)

const selectConfigValueSQL = "SELECT `value` FROM `config` WHERE `key` = ?"

var ErrConfigNotFound = errors.New("config value not found")

type ConfigStore struct {
	db    *sql.DB
	mu    sync.RWMutex
	cache map[string]string
}

func NewConfigStore(db *sql.DB) *ConfigStore {
	return &ConfigStore{
		db:    db,
		cache: make(map[string]string),
	}
}

func (s *ConfigStore) Get(ctx context.Context, key string) (string, error) {
	s.mu.RLock()
	value, ok := s.cache[key]
	s.mu.RUnlock()
	if ok {
		return value, nil
	}

	value, err := s.fetch(ctx, key)
	if err != nil {
		return "", err
	}

	s.mu.Lock()
	s.cache[key] = value
	s.mu.Unlock()
	return value, nil
}

func (s *ConfigStore) fetch(ctx context.Context, key string) (string, error) {
	var value string
	err := s.db.QueryRowContext(ctx, selectConfigValueSQL, key).Scan(&value)
	if err == nil {
		return value, nil
	}
	if errors.Is(err, sql.ErrNoRows) {
		return "", fmt.Errorf("%w: %s", ErrConfigNotFound, key)
	}
	return "", fmt.Errorf("select config %s: %w", key, err)
}
