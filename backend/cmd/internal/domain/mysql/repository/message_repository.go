package repository

import (
	"context"
	"errors"

	"gorm.io/gorm"
	"oficina-img/internal/domain/entity"
)

var ErrMessageNotFound = errors.New("message not found")

type MessageFilter struct {
	Limit    int
	BeforeID *int64
	AfterID  *int64
	AroundID *int64
}

type MessagePage struct {
	Messages      []entity.Message
	HasMoreBefore bool
	HasMoreAfter  bool
}

type MessageRepository struct {
	db *gorm.DB
}

func NewMessageRepository(db *gorm.DB) *MessageRepository {
	return &MessageRepository{db: db}
}

func (r *MessageRepository) List(ctx context.Context, channelID int64, filter MessageFilter) (MessagePage, error) {
	if countMessageAnchors(filter) > 1 {
		return MessagePage{}, errors.New("before, after, and around are mutually exclusive")
	}
	limit := normalizedLimit(filter.Limit, 50, 100)
	var keys []messageKey
	var hasMoreBefore, hasMoreAfter bool
	var err error

	switch {
	case filter.AroundID != nil:
		keys, hasMoreBefore, hasMoreAfter, err = r.keysAround(ctx, channelID, *filter.AroundID, limit)
	case filter.AfterID != nil:
		keys, hasMoreBefore, hasMoreAfter, err = r.keysAfter(ctx, channelID, *filter.AfterID, limit)
	default:
		keys, hasMoreBefore, hasMoreAfter, err = r.keysBefore(ctx, channelID, filter.BeforeID, limit)
	}
	if err != nil {
		return MessagePage{}, err
	}
	if len(keys) == 0 {
		return MessagePage{Messages: []entity.Message{}, HasMoreBefore: hasMoreBefore, HasMoreAfter: hasMoreAfter}, nil
	}

	versions, err := r.messageVersions(ctx, channelID, keys)
	if err != nil {
		return MessagePage{}, err
	}
	return MessagePage{
		Messages:      foldMessages(keys, versions),
		HasMoreBefore: hasMoreBefore,
		HasMoreAfter:  hasMoreAfter,
	}, nil
}

func countMessageAnchors(filter MessageFilter) int {
	count := 0
	for _, anchor := range []*int64{filter.BeforeID, filter.AfterID, filter.AroundID} {
		if anchor != nil {
			count++
		}
	}
	return count
}

func (r *MessageRepository) ListVersions(ctx context.Context, channelID, messageID int64) ([]entity.MessageVersion, error) {
	var rows []messageVersionRow
	err := r.db.WithContext(ctx).
		Table("messages_versions").
		Select("message_id, author_id, message_ref_id, content, sticker_id, created_at").
		Where("channel_id = ? AND message_id = ? AND is_deleted = ?", channelID, messageID, false).
		Order("created_at ASC, id ASC").
		Scan(&rows).Error
	if err != nil {
		return nil, err
	}
	if len(rows) == 0 {
		return nil, ErrMessageNotFound
	}
	versions := make([]entity.MessageVersion, 0, len(rows))
	for _, row := range rows {
		versions = append(versions, entity.MessageVersion{
			MessageID: row.MessageID, AuthorID: row.AuthorID,
			MessageReferenceID: row.MessageReferenceID, Content: row.Content,
			StickerID: row.StickerID, CreatedAt: row.CreatedAt,
		})
	}
	return versions, nil
}

func (r *MessageRepository) keysBefore(ctx context.Context, channelID int64, beforeID *int64, limit int) ([]messageKey, bool, bool, error) {
	query := r.messageKeysQuery(ctx, channelID)
	if beforeID != nil {
		anchor, err := r.messageKey(ctx, channelID, *beforeID)
		if err != nil {
			return nil, false, false, err
		}
		query = query.Having("created_at < ? OR (created_at = ? AND message_id < ?)",
			anchor.CreatedAt, anchor.CreatedAt, anchor.MessageID)
	}
	var keys []messageKey
	if err := query.Order("created_at DESC, message_id DESC").Limit(limit + 1).Scan(&keys).Error; err != nil {
		return nil, false, false, err
	}
	hasMoreBefore := len(keys) > limit
	if hasMoreBefore {
		keys = keys[:limit]
	}
	reverseMessageKeys(keys)
	return keys, hasMoreBefore, beforeID != nil, nil
}

func (r *MessageRepository) keysAfter(ctx context.Context, channelID, afterID int64, limit int) ([]messageKey, bool, bool, error) {
	anchor, err := r.messageKey(ctx, channelID, afterID)
	if err != nil {
		return nil, false, false, err
	}
	var keys []messageKey
	err = r.messageKeysQuery(ctx, channelID).
		Having("created_at > ? OR (created_at = ? AND message_id > ?)", anchor.CreatedAt, anchor.CreatedAt, anchor.MessageID).
		Order("created_at ASC, message_id ASC").
		Limit(limit + 1).
		Scan(&keys).Error
	if err != nil {
		return nil, false, false, err
	}
	hasMoreAfter := len(keys) > limit
	if hasMoreAfter {
		keys = keys[:limit]
	}
	return keys, true, hasMoreAfter, nil
}

func (r *MessageRepository) keysAround(ctx context.Context, channelID, aroundID int64, limit int) ([]messageKey, bool, bool, error) {
	anchor, err := r.messageKey(ctx, channelID, aroundID)
	if err != nil {
		return nil, false, false, err
	}
	var older []messageKey
	err = r.messageKeysQuery(ctx, channelID).
		Having("created_at < ? OR (created_at = ? AND message_id < ?)", anchor.CreatedAt, anchor.CreatedAt, anchor.MessageID).
		Order("created_at DESC, message_id DESC").
		Limit(limit + 1).
		Scan(&older).Error
	if err != nil {
		return nil, false, false, err
	}
	var newer []messageKey
	err = r.messageKeysQuery(ctx, channelID).
		Having("created_at > ? OR (created_at = ? AND message_id > ?)", anchor.CreatedAt, anchor.CreatedAt, anchor.MessageID).
		Order("created_at ASC, message_id ASC").
		Limit(limit + 1).
		Scan(&newer).Error
	if err != nil {
		return nil, false, false, err
	}

	olderCount := min(len(older), (limit-1)/2)
	newerCount := min(len(newer), limit-1-olderCount)
	olderCount = min(len(older), limit-1-newerCount)
	newerCount = min(len(newer), limit-1-olderCount)

	selectedOlder := append([]messageKey(nil), older[:olderCount]...)
	reverseMessageKeys(selectedOlder)
	keys := make([]messageKey, 0, olderCount+1+newerCount)
	keys = append(keys, selectedOlder...)
	keys = append(keys, anchor)
	keys = append(keys, newer[:newerCount]...)
	return keys, len(older) > olderCount, len(newer) > newerCount, nil
}

func (r *MessageRepository) messageKeysQuery(ctx context.Context, channelID int64) *gorm.DB {
	return r.db.WithContext(ctx).
		Table("messages_versions").
		Select("message_id, MIN(created_at) AS created_at").
		Where("channel_id = ?", channelID).
		Group("message_id")
}

func (r *MessageRepository) messageKey(ctx context.Context, channelID, messageID int64) (messageKey, error) {
	var key messageKey
	result := r.messageKeysQuery(ctx, channelID).Having("message_id = ?", messageID).Limit(1).Scan(&key)
	if result.Error != nil {
		return messageKey{}, result.Error
	}
	if result.RowsAffected == 0 {
		return messageKey{}, ErrMessageNotFound
	}
	return key, nil
}

func (r *MessageRepository) messageVersions(ctx context.Context, channelID int64, keys []messageKey) ([]messageVersionRow, error) {
	ids := make([]int64, 0, len(keys))
	for _, key := range keys {
		ids = append(ids, key.MessageID)
	}
	versions := make([]messageVersionRow, 0)
	err := r.db.WithContext(ctx).
		Table("messages_versions AS mv").
		Select(`mv.message_id, mv.author_id, mv.message_ref_id, mv.content, mv.sticker_id,
			mv.is_deleted, mv.is_original, mv.deleted_by_id, mv.created_at`).
		Where("mv.channel_id = ?", channelID).
		Where("mv.message_id IN ?", ids).
		Order("mv.created_at ASC, mv.id ASC").
		Scan(&versions).Error
	if err != nil {
		return nil, err
	}
	return versions, nil
}

func foldMessages(keys []messageKey, versions []messageVersionRow) []entity.Message {
	messagesByID := make(map[int64]*entity.Message, len(keys))
	for _, version := range versions {
		message := messagesByID[version.MessageID]
		if message == nil {
			message = &entity.Message{
				MessageID: version.MessageID, AuthorID: version.AuthorID,
				MessageReferenceID: version.MessageReferenceID, Content: version.Content,
				StickerID: version.StickerID, CreatedAt: version.CreatedAt, UpdatedAt: version.CreatedAt, RevisionCount: 1,
			}
			messagesByID[version.MessageID] = message
			continue
		}
		message.UpdatedAt = version.CreatedAt
		if version.IsDeleted {
			message.IsDeleted = true
			message.DeletedByID = version.DeletedByID
			continue
		}
		message.RevisionCount++
		message.Content = version.Content
		message.StickerID = version.StickerID
		message.MessageReferenceID = version.MessageReferenceID
		message.IsEdited = true
	}
	messages := make([]entity.Message, 0, len(keys))
	for _, key := range keys {
		if message := messagesByID[key.MessageID]; message != nil {
			messages = append(messages, *message)
		}
	}
	return messages
}

func reverseMessageKeys(keys []messageKey) {
	for left, right := 0, len(keys)-1; left < right; left, right = left+1, right-1 {
		keys[left], keys[right] = keys[right], keys[left]
	}
}

type messageKey struct {
	MessageID int64
	CreatedAt int64
}

type messageVersionRow struct {
	MessageID          int64
	AuthorID           int64
	MessageReferenceID *int64 `gorm:"column:message_ref_id"`
	Content            *string
	StickerID          *int64
	IsDeleted          bool
	IsOriginal         bool
	DeletedByID        *int64
	CreatedAt          int64
}
