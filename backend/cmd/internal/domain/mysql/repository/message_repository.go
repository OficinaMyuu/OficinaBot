package repository

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"strings"

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
	db *sql.DB
}

func NewMessageRepository(db *sql.DB) *MessageRepository {
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
	rows, err := r.db.QueryContext(ctx, `
SELECT message_id, author_id, message_ref_id, content, sticker_id, created_at
FROM messages_versions
WHERE channel_id = ? AND message_id = ? AND is_deleted = FALSE
ORDER BY created_at ASC, id ASC`, channelID, messageID)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	versions := make([]entity.MessageVersion, 0)
	for rows.Next() {
		var version entity.MessageVersion
		var referenceID, stickerID sql.NullInt64
		var content sql.NullString
		if err := rows.Scan(&version.MessageID, &version.AuthorID, &referenceID, &content, &stickerID, &version.CreatedAt); err != nil {
			return nil, err
		}
		version.MessageReferenceID = nullableInt64(referenceID)
		version.Content = nullableString(content)
		version.StickerID = nullableInt64(stickerID)
		versions = append(versions, version)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}
	if len(versions) == 0 {
		return nil, ErrMessageNotFound
	}
	return versions, nil
}

func (r *MessageRepository) keysBefore(ctx context.Context, channelID int64, beforeID *int64, limit int) ([]messageKey, bool, bool, error) {
	query := strings.Builder{}
	query.WriteString(messageKeysQuery)
	args := []any{channelID}
	if beforeID != nil {
		anchor, err := r.messageKey(ctx, channelID, *beforeID)
		if err != nil {
			return nil, false, false, err
		}
		query.WriteString(" HAVING created_at < ? OR (created_at = ? AND message_id < ?)")
		args = append(args, anchor.CreatedAt, anchor.CreatedAt, anchor.MessageID)
	}
	query.WriteString(" ORDER BY created_at DESC, message_id DESC LIMIT ?")
	args = append(args, limit+1)
	keys, err := r.queryMessageKeys(ctx, query.String(), args...)
	if err != nil {
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
	query := messageKeysQuery + " HAVING created_at > ? OR (created_at = ? AND message_id > ?) ORDER BY created_at ASC, message_id ASC LIMIT ?"
	keys, err := r.queryMessageKeys(ctx, query, channelID, anchor.CreatedAt, anchor.CreatedAt, anchor.MessageID, limit+1)
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
	olderQuery := messageKeysQuery + " HAVING created_at < ? OR (created_at = ? AND message_id < ?) ORDER BY created_at DESC, message_id DESC LIMIT ?"
	older, err := r.queryMessageKeys(ctx, olderQuery, channelID, anchor.CreatedAt, anchor.CreatedAt, anchor.MessageID, limit+1)
	if err != nil {
		return nil, false, false, err
	}
	newerQuery := messageKeysQuery + " HAVING created_at > ? OR (created_at = ? AND message_id > ?) ORDER BY created_at ASC, message_id ASC LIMIT ?"
	newer, err := r.queryMessageKeys(ctx, newerQuery, channelID, anchor.CreatedAt, anchor.CreatedAt, anchor.MessageID, limit+1)
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

const messageKeysQuery = `
SELECT message_id, MIN(created_at) AS created_at
FROM messages_versions
WHERE channel_id = ?
GROUP BY message_id`

func (r *MessageRepository) messageKey(ctx context.Context, channelID, messageID int64) (messageKey, error) {
	var key messageKey
	err := r.db.QueryRowContext(ctx, messageKeysQuery+" HAVING message_id = ?", channelID, messageID).Scan(&key.MessageID, &key.CreatedAt)
	if errors.Is(err, sql.ErrNoRows) {
		return messageKey{}, ErrMessageNotFound
	}
	return key, err
}

func (r *MessageRepository) queryMessageKeys(ctx context.Context, query string, args ...any) ([]messageKey, error) {
	rows, err := r.db.QueryContext(ctx, query, args...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	keys := make([]messageKey, 0)
	for rows.Next() {
		var key messageKey
		if err := rows.Scan(&key.MessageID, &key.CreatedAt); err != nil {
			return nil, err
		}
		keys = append(keys, key)
	}
	return keys, rows.Err()
}

func (r *MessageRepository) messageVersions(ctx context.Context, channelID int64, keys []messageKey) ([]messageVersionRow, error) {
	ids := make([]any, 0, len(keys)+1)
	placeholders := make([]string, 0, len(keys))
	for _, key := range keys {
		ids = append(ids, key.MessageID)
		placeholders = append(placeholders, "?")
	}
	args := append([]any{channelID}, ids...)
	query := fmt.Sprintf(`
SELECT mv.message_id, mv.author_id, mv.message_ref_id, mv.content, mv.sticker_id,
       mv.is_deleted, mv.is_original, mv.deleted_by_id, mv.created_at
FROM messages_versions mv
WHERE mv.channel_id = ? AND mv.message_id IN (%s)
ORDER BY mv.created_at ASC, mv.id ASC`, strings.Join(placeholders, ", "))
	rows, err := r.db.QueryContext(ctx, query, args...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	versions := make([]messageVersionRow, 0)
	for rows.Next() {
		var row messageVersionRow
		if err := rows.Scan(&row.MessageID, &row.AuthorID, &row.MessageReferenceID, &row.Content, &row.StickerID, &row.IsDeleted, &row.IsOriginal, &row.DeletedByID, &row.CreatedAt); err != nil {
			return nil, err
		}
		versions = append(versions, row)
	}
	return versions, rows.Err()
}

func foldMessages(keys []messageKey, versions []messageVersionRow) []entity.Message {
	messagesByID := make(map[int64]*entity.Message, len(keys))
	for _, version := range versions {
		message := messagesByID[version.MessageID]
		if message == nil {
			message = &entity.Message{
				MessageID: version.MessageID, AuthorID: version.AuthorID,
				MessageReferenceID: nullableInt64(version.MessageReferenceID), Content: nullableString(version.Content),
				StickerID: nullableInt64(version.StickerID), CreatedAt: version.CreatedAt, UpdatedAt: version.CreatedAt, RevisionCount: 1,
			}
			messagesByID[version.MessageID] = message
			continue
		}
		message.UpdatedAt = version.CreatedAt
		if version.IsDeleted {
			message.IsDeleted = true
			message.DeletedByID = nullableInt64(version.DeletedByID)
			continue
		}
		message.RevisionCount++
		message.Content = nullableString(version.Content)
		message.StickerID = nullableInt64(version.StickerID)
		message.MessageReferenceID = nullableInt64(version.MessageReferenceID)
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
	MessageReferenceID sql.NullInt64
	Content            sql.NullString
	StickerID          sql.NullInt64
	IsDeleted          bool
	IsOriginal         bool
	DeletedByID        sql.NullInt64
	CreatedAt          int64
}
