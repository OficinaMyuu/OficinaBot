package repository

import (
	"context"
	"errors"
	"os"
	"testing"
)

func TestStoreItemSettingsRepositoryIntegration(t *testing.T) {
	dsn := os.Getenv("OFICINA_TEST_MYSQL_DSN")
	if dsn == "" {
		t.Skip("set OFICINA_TEST_MYSQL_DSN to run live MySQL store item settings repository coverage")
	}

	db := openTemporaryMySQLSchema(t, dsn)
	if _, err := db.Exec(`
CREATE TABLE store_item_settings (
    item_type VARCHAR(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    price INT NOT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    updated_by BIGINT NULL,
    PRIMARY KEY (item_type),
    CONSTRAINT chk_test_store_item_type CHECK (item_type IN ('GROUP', 'GROUP_SLOT')),
    CONSTRAINT chk_test_store_item_price CHECK (price >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci`); err != nil {
		t.Fatalf("create store item settings table: %v", err)
	}
	if _, err := db.Exec(`
INSERT INTO store_item_settings (item_type, price, created_at, updated_at) VALUES
('GROUP', 600000, 100, 100),
('GROUP_SLOT', 75000, 100, 100)`); err != nil {
		t.Fatalf("seed store item settings: %v", err)
	}

	repository := NewStoreItemSettingsRepository(db)
	ctx := context.Background()
	items, err := repository.List(ctx)
	if err != nil {
		t.Fatalf("list settings: %v", err)
	}
	if len(items) != 2 || items[0].ItemType != "GROUP" || items[1].ItemType != "GROUP_SLOT" {
		t.Fatalf("unexpected settings: %+v", items)
	}

	updated, err := repository.Update(ctx, "GROUP", 0, 42)
	if err != nil {
		t.Fatalf("update setting: %v", err)
	}
	if updated.Price != 0 || updated.UpdatedBy == nil || *updated.UpdatedBy != 42 {
		t.Fatalf("unexpected updated setting: %+v", updated)
	}

	updated, err = repository.Update(ctx, "GROUP", 10, 42)
	if err != nil || updated.Price != 10 {
		t.Fatalf("expected last write to win, got setting %+v and error %v", updated, err)
	}
	if _, err := repository.Update(ctx, "UNKNOWN", 10, 42); !errors.Is(err, ErrStoreItemSettingNotFound) {
		t.Fatalf("expected missing setting, got %v", err)
	}
}
