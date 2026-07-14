package repository

import (
	"context"
	"errors"
	"time"

	"gorm.io/gorm"
	"oficina-img/internal/domain/entity"
)

var (
	ErrStoreItemSettingNotFound = errors.New("store item setting not found")
)

type StoreItemSettingsRepository struct {
	db *gorm.DB
}

func NewStoreItemSettingsRepository(db *gorm.DB) *StoreItemSettingsRepository {
	return &StoreItemSettingsRepository{db: db}
}

func (r *StoreItemSettingsRepository) List(ctx context.Context) ([]entity.StoreItemSetting, error) {
	var rows []storeItemSettingRow
	if err := r.db.WithContext(ctx).Order("item_type ASC").Find(&rows).Error; err != nil {
		return nil, err
	}
	items := make([]entity.StoreItemSetting, 0, len(rows))
	for _, row := range rows {
		items = append(items, row.entity())
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
	result := r.db.WithContext(ctx).Model(&storeItemSettingRow{}).
		Where("item_type = ?", itemType).
		Updates(map[string]any{"price": price, "updated_at": now, "updated_by": updatedBy})
	if result.Error != nil {
		return entity.StoreItemSetting{}, result.Error
	}

	return r.find(ctx, itemType)
}

func (r *StoreItemSettingsRepository) find(ctx context.Context, itemType string) (entity.StoreItemSetting, error) {
	var row storeItemSettingRow
	err := r.db.WithContext(ctx).Where("item_type = ?", itemType).Take(&row).Error
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return entity.StoreItemSetting{}, ErrStoreItemSettingNotFound
	}
	if err != nil {
		return entity.StoreItemSetting{}, err
	}
	return row.entity(), nil
}
