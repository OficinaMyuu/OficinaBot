package discord

import (
	"testing"
	"time"
)

func TestCacheReturnsStoredValueBeforeExpiry(t *testing.T) {
	now := time.Unix(100, 0).UTC()
	cache := NewCache[string](time.Minute)
	cache.now = func() time.Time { return now }

	cache.Set("guild", "Oficina")

	got, ok := cache.Get("guild")
	if !ok {
		t.Fatal("expected cache hit")
	}
	if got != "Oficina" {
		t.Fatalf("expected Oficina, got %q", got)
	}
}

func TestCacheExpiresStoredValue(t *testing.T) {
	now := time.Unix(100, 0).UTC()
	cache := NewCache[string](time.Minute)
	cache.now = func() time.Time { return now }
	cache.Set("guild", "Oficina")

	now = now.Add(2 * time.Minute)

	if _, ok := cache.Get("guild"); ok {
		t.Fatal("expected cache miss after expiry")
	}
}
