package repository

import (
	"context"

	"gorm.io/gorm"
	"oficina-img/internal/domain/entity"
)

type UserRepository struct {
	db *gorm.DB
}

func NewUserRepository(db *gorm.DB) *UserRepository {
	return &UserRepository{db: db}
}

func (r *UserRepository) FindMany(ctx context.Context, userIDs []int64) ([]entity.User, error) {
	if len(userIDs) == 0 {
		return []entity.User{}, nil
	}

	var rows []userRow
	if err := r.db.WithContext(ctx).
		Where("id IN ?", userIDs).
		Order("name ASC, id ASC").
		Find(&rows).Error; err != nil {
		return nil, err
	}
	users := make([]entity.User, 0, len(rows))
	for _, row := range rows {
		users = append(users, row.entity())
	}
	return users, nil
}
