package repository

import (
	"context"
	"errors"
	"time"

	"gorm.io/gorm"
	"gorm.io/gorm/clause"
	"oficina-img/internal/domain/entity"
)

var ErrSessionNotFound = errors.New("dashboard session not found")

type DashboardSessionRepository struct {
	db *gorm.DB
}

func NewDashboardSessionRepository(db *gorm.DB) *DashboardSessionRepository {
	return &DashboardSessionRepository{db: db}
}

func (r *DashboardSessionRepository) Save(ctx context.Context, sessionIDHash string, session entity.DashboardSession) error {
	now := time.Now().Unix()
	row := dashboardSessionRow{
		SessionIDHash: sessionIDHash, CSRFToken: session.CSRFToken,
		UserID: session.User.ID, Username: session.User.Username,
		GlobalName: session.User.GlobalName, AvatarURL: session.User.AvatarURL,
		GuildName: session.User.GuildName, GuildIconURL: session.User.GuildIconURL,
		Permissions: session.User.Permissions, ExpiresAt: session.ExpiresAt,
		CreatedAt: now, UpdatedAt: now,
	}
	return r.db.WithContext(ctx).Clauses(clause.OnConflict{
		Columns: []clause.Column{{Name: "session_id_hash"}},
		DoUpdates: clause.AssignmentColumns([]string{
			"csrf_token", "user_id", "username", "global_name", "avatar_url",
			"guild_name", "guild_icon_url", "permissions", "expires_at", "updated_at",
		}),
	}).Create(&row).Error
}

func (r *DashboardSessionRepository) Find(ctx context.Context, sessionIDHash string) (entity.DashboardSession, error) {
	var row dashboardSessionRow
	err := r.db.WithContext(ctx).Where("session_id_hash = ?", sessionIDHash).Take(&row).Error
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return entity.DashboardSession{}, ErrSessionNotFound
	}
	if err != nil {
		return entity.DashboardSession{}, err
	}
	return row.entity(), nil
}

func (r *DashboardSessionRepository) Delete(ctx context.Context, sessionIDHash string) error {
	return r.db.WithContext(ctx).Where("session_id_hash = ?", sessionIDHash).Delete(&dashboardSessionRow{}).Error
}

func (r *DashboardSessionRepository) DeleteExpired(ctx context.Context, now int64) error {
	return r.db.WithContext(ctx).Where("expires_at <= ?", now).Delete(&dashboardSessionRow{}).Error
}
