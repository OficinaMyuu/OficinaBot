package repository

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"strconv"
	"strings"

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
	db *sql.DB
}

func NewTicketRepository(db *sql.DB) *TicketRepository {
	return &TicketRepository{db: db}
}

func (r *TicketRepository) List(ctx context.Context, filter TicketListFilter) (TicketPage, error) {
	limit := normalizedLimit(filter.Limit, 25, 100)
	query := strings.Builder{}
	query.WriteString(`
SELECT st.id, st.title, st.description, st.guild_id, st.channel_id, st.initiator_id,
       st.close_reason, st.closed_by_id, st.merged_into, st.created_at, st.updated_at
FROM support_tickets st
LEFT JOIN users initiator ON initiator.id = st.initiator_id
WHERE 1 = 1`)
	args := make([]any, 0, 8)

	switch filter.Status {
	case "open":
		query.WriteString(" AND st.closed_by_id IS NULL")
	case "closed":
		query.WriteString(" AND st.closed_by_id IS NOT NULL")
	}

	if search := strings.TrimSpace(filter.Search); search != "" {
		needle := "%" + strings.ToLower(search) + "%"
		query.WriteString(` AND (
LOWER(st.title) LIKE ?
OR LOWER(st.description) LIKE ?
OR CAST(st.id AS CHAR) LIKE ?
OR CAST(st.channel_id AS CHAR) LIKE ?
OR CAST(st.initiator_id AS CHAR) LIKE ?
OR LOWER(initiator.name) LIKE ?
OR LOWER(initiator.global_name) LIKE ?
)`)
		args = append(args, needle, needle, "%"+search+"%", "%"+search+"%", "%"+search+"%", needle, needle)
	}

	if filter.Cursor != nil {
		query.WriteString(" AND (st.created_at < ? OR (st.created_at = ? AND st.id < ?))")
		args = append(args, filter.Cursor.CreatedAt, filter.Cursor.CreatedAt, filter.Cursor.ID)
	}

	query.WriteString(" ORDER BY st.created_at DESC, st.id DESC LIMIT ?")
	args = append(args, limit+1)

	rows, err := r.db.QueryContext(ctx, query.String(), args...)
	if err != nil {
		return TicketPage{}, err
	}
	defer rows.Close()

	tickets, err := scanTickets(rows)
	if err != nil {
		return TicketPage{}, err
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
	rows, err := r.db.QueryContext(ctx, `
SELECT st.id, st.title, st.description, st.guild_id, st.channel_id, st.initiator_id,
       st.close_reason, st.closed_by_id, st.merged_into, st.created_at, st.updated_at
FROM support_tickets st
WHERE st.id = ?
LIMIT 1`, ticketID)
	if err != nil {
		return entity.Ticket{}, err
	}
	defer rows.Close()

	tickets, err := scanTickets(rows)
	if err != nil {
		return entity.Ticket{}, err
	}
	if len(tickets) == 0 {
		return entity.Ticket{}, ErrTicketNotFound
	}
	return tickets[0], nil
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

type ticketScanner interface {
	Scan(dest ...any) error
}

func scanTickets(rows interface {
	Next() bool
	ticketScanner
	Err() error
}) ([]entity.Ticket, error) {
	tickets := make([]entity.Ticket, 0)
	for rows.Next() {
		var ticket entity.Ticket
		var closeReason sql.NullString
		var closedByID sql.NullInt64
		var mergedInto sql.NullInt64

		if err := rows.Scan(
			&ticket.ID,
			&ticket.Title,
			&ticket.Description,
			&ticket.GuildID,
			&ticket.ChannelID,
			&ticket.InitiatorID,
			&closeReason,
			&closedByID,
			&mergedInto,
			&ticket.CreatedAt,
			&ticket.UpdatedAt,
		); err != nil {
			return nil, err
		}

		ticket.CloseReason = nullableString(closeReason)
		ticket.ClosedByID = nullableInt64(closedByID)
		if mergedInto.Valid {
			value := int(mergedInto.Int64)
			ticket.MergedInto = &value
		}
		tickets = append(tickets, ticket)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}
	return tickets, nil
}

func nullableString(value sql.NullString) *string {
	if !value.Valid {
		return nil
	}
	return &value.String
}

func nullableInt64(value sql.NullInt64) *int64 {
	if !value.Valid {
		return nil
	}
	return &value.Int64
}
