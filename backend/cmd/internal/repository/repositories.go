package repository

import (
	"context"
	"errors"
	"time"

	"gorm.io/gorm"
)

type UserRepository struct {
	db *gorm.DB
}

func NewUserRepository(db *gorm.DB) *UserRepository {
	return &UserRepository{db: db}
}

func (r *UserRepository) Create(ctx context.Context, user *User) error {
	setCreatedAt(&user.CreatedAt)
	return r.db.WithContext(ctx).Create(user).Error
}

func (r *UserRepository) Get(ctx context.Context, discordID string) (*User, error) {
	var user User
	if err := r.db.WithContext(ctx).First(&user, "discord_id = ?", discordID).Error; err != nil {
		return nil, err
	}
	return &user, nil
}

func (r *UserRepository) List(ctx context.Context) ([]User, error) {
	var users []User
	err := r.db.WithContext(ctx).Order("created_at ASC").Find(&users).Error
	return users, err
}

func (r *UserRepository) Delete(ctx context.Context, discordID string) error {
	return r.db.WithContext(ctx).Delete(&User{}, "discord_id = ?", discordID).Error
}

type BotClientRepository struct {
	db *gorm.DB
}

func NewBotClientRepository(db *gorm.DB) *BotClientRepository {
	return &BotClientRepository{db: db}
}

func (r *BotClientRepository) Create(ctx context.Context, client *BotClient) error {
	setCreatedAt(&client.CreatedAt)
	return r.db.WithContext(ctx).Create(client).Error
}

func (r *BotClientRepository) Upsert(ctx context.Context, client *BotClient) error {
	existing, err := r.Get(ctx, client.Name)
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return r.Create(ctx, client)
		}
		return err
	}

	updates := map[string]any{
		"token_hash": client.TokenHash,
	}
	if client.LastSeenAt != nil {
		updates["last_seen_at"] = client.LastSeenAt
	}
	return r.db.WithContext(ctx).
		Model(existing).
		Updates(updates).
		Error
}

func (r *BotClientRepository) Get(ctx context.Context, name string) (*BotClient, error) {
	var client BotClient
	if err := r.db.WithContext(ctx).First(&client, "name = ?", name).Error; err != nil {
		return nil, err
	}
	return &client, nil
}

func (r *BotClientRepository) GetByTokenHash(ctx context.Context, tokenHash string) (*BotClient, error) {
	var client BotClient
	if err := r.db.WithContext(ctx).First(&client, "token_hash = ?", tokenHash).Error; err != nil {
		return nil, err
	}
	return &client, nil
}

func (r *BotClientRepository) Touch(ctx context.Context, name string, seenAt time.Time) error {
	return r.db.WithContext(ctx).
		Model(&BotClient{}).
		Where("name = ?", name).
		Update("last_seen_at", seenAt).
		Error
}

type EventBatchRepository struct {
	db *gorm.DB
}

func NewEventBatchRepository(db *gorm.DB) *EventBatchRepository {
	return &EventBatchRepository{db: db}
}

func (r *EventBatchRepository) Create(ctx context.Context, batch *EventBatch) error {
	setCreatedAt(&batch.ReceivedAt)
	return r.db.WithContext(ctx).Create(batch).Error
}

type MessageLogRepository struct {
	db *gorm.DB
}

func NewMessageLogRepository(db *gorm.DB) *MessageLogRepository {
	return &MessageLogRepository{db: db}
}

func (r *MessageLogRepository) CreateMany(ctx context.Context, logs []MessageLog) error {
	now := time.Now().UTC()
	for i := range logs {
		if logs[i].CreatedAt.IsZero() {
			logs[i].CreatedAt = now
		}
	}
	return r.db.WithContext(ctx).Create(&logs).Error
}

type PunishmentRepository struct {
	db *gorm.DB
}

func NewPunishmentRepository(db *gorm.DB) *PunishmentRepository {
	return &PunishmentRepository{db: db}
}

func (r *PunishmentRepository) Create(ctx context.Context, punishment *Punishment) error {
	setCreatedAt(&punishment.CreatedAt)
	return r.db.WithContext(ctx).Create(punishment).Error
}

func (r *PunishmentRepository) ListRecent(ctx context.Context, limit int) ([]Punishment, error) {
	var punishments []Punishment
	err := r.db.WithContext(ctx).
		Order("created_at DESC").
		Limit(normalizeLimit(limit)).
		Find(&punishments).
		Error
	return punishments, err
}

type ConfigVersionRepository struct {
	db *gorm.DB
}

func NewConfigVersionRepository(db *gorm.DB) *ConfigVersionRepository {
	return &ConfigVersionRepository{db: db}
}

func (r *ConfigVersionRepository) Create(ctx context.Context, version *ConfigVersion) error {
	setCreatedAt(&version.CreatedAt)
	return r.db.WithContext(ctx).Create(version).Error
}

func (r *ConfigVersionRepository) PendingForClient(ctx context.Context, clientName string) ([]ConfigVersion, error) {
	var versions []ConfigVersion
	err := r.db.WithContext(ctx).
		Joins("LEFT JOIN config_acknowledgements ON config_acknowledgements.config_version_id = config_versions.id AND config_acknowledgements.bot_client_name = ?", clientName).
		Where("config_acknowledgements.id IS NULL").
		Order("config_versions.id ASC").
		Find(&versions).
		Error
	return versions, err
}

func (r *ConfigVersionRepository) Acknowledge(ctx context.Context, versionID int64, clientName string) error {
	ack := &ConfigAcknowledgement{
		ConfigVersionID: versionID,
		BotClientName:   clientName,
		AckedAt:         time.Now().UTC(),
	}
	return r.db.WithContext(ctx).Create(ack).Error
}

type AuditActionRepository struct {
	db *gorm.DB
}

func NewAuditActionRepository(db *gorm.DB) *AuditActionRepository {
	return &AuditActionRepository{db: db}
}

func (r *AuditActionRepository) Create(ctx context.Context, action *AuditAction) error {
	setCreatedAt(&action.CreatedAt)
	if action.MetadataJSON == "" {
		action.MetadataJSON = "{}"
	}
	return r.db.WithContext(ctx).Create(action).Error
}

func (r *AuditActionRepository) ListRecent(ctx context.Context, limit int) ([]AuditAction, error) {
	var actions []AuditAction
	err := r.db.WithContext(ctx).
		Order("created_at DESC").
		Limit(normalizeLimit(limit)).
		Find(&actions).
		Error
	return actions, err
}

func setCreatedAt(createdAt *time.Time) {
	if createdAt.IsZero() {
		*createdAt = time.Now().UTC()
	}
}

func normalizeLimit(limit int) int {
	if limit <= 0 || limit > 100 {
		return 100
	}
	return limit
}

type AdminSessionRepository struct {
	db *gorm.DB
}

func NewAdminSessionRepository(db *gorm.DB) *AdminSessionRepository {
	return &AdminSessionRepository{db: db}
}

func (r *AdminSessionRepository) Create(ctx context.Context, session *AdminSession) error {
	now := time.Now().UTC()
	if session.CreatedAt.IsZero() {
		session.CreatedAt = now
	}
	if session.LastSeenAt.IsZero() {
		session.LastSeenAt = now
	}
	return r.db.WithContext(ctx).Create(session).Error
}

func (r *AdminSessionRepository) GetValid(ctx context.Context, tokenHash string, now time.Time) (*AdminSession, error) {
	var session AdminSession
	if err := r.db.WithContext(ctx).
		Preload("User").
		Where("token_hash = ? AND expires_at > ?", tokenHash, now.UTC()).
		First(&session).
		Error; err != nil {
		return nil, err
	}
	return &session, nil
}

func (r *AdminSessionRepository) Touch(ctx context.Context, tokenHash string, seenAt time.Time) error {
	return r.db.WithContext(ctx).
		Model(&AdminSession{}).
		Where("token_hash = ?", tokenHash).
		Update("last_seen_at", seenAt.UTC()).
		Error
}

func (r *AdminSessionRepository) Delete(ctx context.Context, tokenHash string) error {
	return r.db.WithContext(ctx).Delete(&AdminSession{}, "token_hash = ?", tokenHash).Error
}
