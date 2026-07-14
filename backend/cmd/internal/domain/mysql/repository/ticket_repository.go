package repository

import (
	"context"
	"errors"
	"fmt"
	"strconv"
	"strings"

	"gorm.io/gorm"
	"oficina-img/internal/domain/entity"
)

var ErrTicketNotFound = errors.New("ticket not found")

type TicketCursor struct {
	CreatedAt int64
	ID        int64
}

type TicketListFilter struct {
	Search string
	Status string
	Limit  int
	Cursor *TicketCursor
}

type TicketPage struct {
	Tickets    []entity.Ticket
	NextCursor *TicketCursor
}

type TicketRepository struct {
	db *gorm.DB
}

func NewTicketRepository(db *gorm.DB) *TicketRepository {
	return &TicketRepository{db: db}
}

func (r *TicketRepository) List(ctx context.Context, filter TicketListFilter) (TicketPage, error) {
	limit := normalizedLimit(filter.Limit, 25, 100)
	query := r.db.WithContext(ctx).
		Table("support_tickets AS st").
		Select(`st.id, st.title, st.description, st.guild_id, st.channel_id, st.initiator_id,
			st.close_reason, st.closed_by_id, st.merged_into, st.created_at, st.updated_at`).
		Joins("LEFT JOIN users AS initiator ON initiator.id = st.initiator_id")

	switch filter.Status {
	case "open":
		query = query.Where("st.closed_by_id IS NULL")
	case "closed":
		query = query.Where("st.closed_by_id IS NOT NULL")
	}

	if search := strings.TrimSpace(filter.Search); search != "" {
		needle := "%" + strings.ToLower(search) + "%"
		idNeedle := "%" + search + "%"
		query = query.Where(`(LOWER(st.title) LIKE ?
			OR LOWER(st.description) LIKE ?
			OR CAST(st.id AS CHAR) LIKE ?
			OR CAST(st.channel_id AS CHAR) LIKE ?
			OR CAST(st.initiator_id AS CHAR) LIKE ?
			OR LOWER(initiator.name) LIKE ?
			OR LOWER(initiator.global_name) LIKE ?)`,
			needle, needle, idNeedle, idNeedle, idNeedle, needle, needle)
	}

	if filter.Cursor != nil {
		query = query.Where("st.created_at < ? OR (st.created_at = ? AND st.id < ?)",
			filter.Cursor.CreatedAt, filter.Cursor.CreatedAt, filter.Cursor.ID)
	}

	var rows []ticketRow
	if err := query.Order("st.created_at DESC, st.id DESC").Limit(limit + 1).Scan(&rows).Error; err != nil {
		return TicketPage{}, err
	}
	tickets := make([]entity.Ticket, 0, len(rows))
	for _, row := range rows {
		tickets = append(tickets, row.entity())
	}

	page := TicketPage{Tickets: tickets}
	if len(page.Tickets) > limit {
		last := page.Tickets[limit-1]
		page.NextCursor = &TicketCursor{CreatedAt: last.CreatedAt, ID: int64(last.ID)}
		page.Tickets = page.Tickets[:limit]
	}
	return page, nil
}

func (r *TicketRepository) Find(ctx context.Context, ticketID int) (entity.Ticket, error) {
	var row ticketRow
	err := r.db.WithContext(ctx).Where("id = ?", ticketID).Take(&row).Error
	if errors.Is(err, gorm.ErrRecordNotFound) {
		return entity.Ticket{}, ErrTicketNotFound
	}
	if err != nil {
		return entity.Ticket{}, err
	}
	return row.entity(), nil
}

func ParseTicketCursor(value string) (*TicketCursor, error) {
	if value == "" {
		return nil, nil
	}
	createdAtRaw, idRaw, ok := strings.Cut(value, ":")
	if !ok {
		return nil, errors.New("cursor must use created_at:id format")
	}
	createdAt, err := strconv.ParseInt(createdAtRaw, 10, 64)
	if err != nil || createdAt < 0 {
		return nil, errors.New("cursor created_at must be a non-negative integer")
	}
	id, err := strconv.ParseInt(idRaw, 10, 64)
	if err != nil || id <= 0 {
		return nil, errors.New("cursor id must be a positive integer")
	}
	return &TicketCursor{CreatedAt: createdAt, ID: id}, nil
}

func FormatTicketCursor(cursor *TicketCursor) *string {
	if cursor == nil {
		return nil
	}
	value := fmt.Sprintf("%d:%d", cursor.CreatedAt, cursor.ID)
	return &value
}

func normalizedLimit(value, fallback, max int) int {
	if value <= 0 {
		return fallback
	}
	if value > max {
		return max
	}
	return value
}
