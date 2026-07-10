package repository

import (
	"context"
	"database/sql"
	"errors"
	"time"

	"oficina-img/internal/domain/entity"
)

var (
	ErrStoreItemSettingNotFound = errors.New("store item setting not found")
)

type StoreItemSettingsRepository struct {
	db *sql.DB
}

func NewStoreItemSettingsRepository(db *sql.DB) *StoreItemSettingsRepository {
	return &StoreItemSettingsRepository{db: db}
}

func (r *StoreItemSettingsRepository) List(ctx context.Context) ([]entity.StoreItemSetting, error) {
	rows, err := r.db.QueryContext(ctx, `
SELECT item_type, price, created_at, updated_at, updated_by
FROM store_item_settings
ORDER BY item_type ASC`)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	items := make([]entity.StoreItemSetting, 0)
	for rows.Next() {
		item, err := scanStoreItemSetting(rows)
		if err != nil {
			return nil, err
		}
		items = append(items, item)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}
	return items, nil
}

func (r *StoreItemSettingsRepository) Update(
	ctx context.Context,
	itemType string,
	price int,
	updatedBy int64,
) (entity.StoreItemSetting, error) {
	now := time.Now().Unix()
	_, err := r.db.ExecContext(ctx, `
UPDATE store_item_settings
SET price = ?, updated_at = ?, updated_by = ?
WHERE item_type = ?`, price, now, updatedBy, itemType)
	if err != nil {
		return entity.StoreItemSetting{}, err
	}

	return r.find(ctx, itemType)
}

func (r *StoreItemSettingsRepository) find(ctx context.Context, itemType string) (entity.StoreItemSetting, error) {
	row := r.db.QueryRowContext(ctx, `
SELECT item_type, price, created_at, updated_at, updated_by
FROM store_item_settings
WHERE item_type = ?`, itemType)
	item, err := scanStoreItemSetting(row)
	if errors.Is(err, sql.ErrNoRows) {
		return entity.StoreItemSetting{}, ErrStoreItemSettingNotFound
	}
	return item, err
}

type storeItemSettingScanner interface {
	Scan(dest ...any) error
}

func scanStoreItemSetting(scanner storeItemSettingScanner) (entity.StoreItemSetting, error) {
	var item entity.StoreItemSetting
	var updatedBy sql.NullInt64
	if err := scanner.Scan(
		&item.ItemType,
		&item.Price,
		&item.CreatedAt,
		&item.UpdatedAt,
		&updatedBy,
	); err != nil {
		return entity.StoreItemSetting{}, err
	}
	if updatedBy.Valid {
		item.UpdatedBy = &updatedBy.Int64
	}
	return item, nil
}
