package discord

import (
	"sync"
	"time"
)

const DefaultCacheTTL = 5 * time.Minute

type Cache[T any] struct {
	ttl   time.Duration
	now   func() time.Time
	mu    sync.RWMutex
	items map[string]cacheEntry[T]
}

type cacheEntry[T any] struct {
	value     T
	expiresAt time.Time
}

func NewCache[T any](ttl time.Duration) *Cache[T] {
	return &Cache[T]{
		ttl:   ttl,
		now:   func() time.Time { return time.Now().UTC() },
		items: make(map[string]cacheEntry[T]),
	}
}

func (c *Cache[T]) Get(key string) (T, bool) {
	c.mu.RLock()
	entry, ok := c.items[key]
	c.mu.RUnlock()

	var zero T
	if !ok {
		return zero, false
	}
	if !entry.expiresAt.After(c.now()) {
		c.mu.Lock()
		delete(c.items, key)
		c.mu.Unlock()
		return zero, false
	}
	return entry.value, true
}

func (c *Cache[T]) Set(key string, value T) {
	c.mu.Lock()
	defer c.mu.Unlock()

	c.items[key] = cacheEntry[T]{
		value:     value,
		expiresAt: c.now().Add(c.ttl),
	}
}
