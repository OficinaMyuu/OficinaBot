package handler

import (
	"context"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/domain/entity"
	"oficina-img/internal/domain/mysql/repository"
)

type fakeTicketRepository struct {
	listFilter      repository.TicketListFilter
	messageFilter   repository.TicketMessageFilter
	findErr         error
	listErr         error
	listMessagesErr error
}

func (f *fakeTicketRepository) List(_ context.Context, filter repository.TicketListFilter) (repository.TicketPage, error) {
	f.listFilter = filter
	if f.listErr != nil {
		return repository.TicketPage{}, f.listErr
	}
	closedBy := int64(99)
	return repository.TicketPage{
		Tickets: []entity.Ticket{{
			ID:          7,
			Title:       "Need help",
			Description: "The thing exploded",
			GuildID:     123,
			ChannelID:   456,
			InitiatorID: 42,
			CloseReason: stringPtr("Solved"),
			ClosedByID:  &closedBy,
			MergedInto:  intPtr(9),
			CreatedAt:   10,
			UpdatedAt:   11,
		}},
		NextCursor: &repository.TicketCursor{CreatedAt: 10, ID: 7},
	}, nil
}

func (f *fakeTicketRepository) Find(_ context.Context, _ int) (entity.Ticket, error) {
	if f.findErr != nil {
		return entity.Ticket{}, f.findErr
	}
	return entity.Ticket{
		ID:          7,
		Title:       "Need help",
		Description: "The thing exploded",
		GuildID:     123,
		ChannelID:   456,
		InitiatorID: 42,
		CreatedAt:   10,
		UpdatedAt:   11,
	}, nil
}

func (f *fakeTicketRepository) ListMessages(_ context.Context, _ int64, filter repository.TicketMessageFilter) (repository.TicketMessagePage, error) {
	f.messageFilter = filter
	if f.listMessagesErr != nil {
		return repository.TicketMessagePage{}, f.listMessagesErr
	}
	ref := int64(100)
	sticker := int64(200)
	return repository.TicketMessagePage{
		Messages: []entity.TicketMessage{{
			MessageID:          101,
			AuthorID:           42,
			MessageReferenceID: &ref,
			Content:            stringPtr("hello"),
			StickerID:          &sticker,
			IsEdited:           true,
			CreatedAt:          1000,
			UpdatedAt:          1001,
		}},
		NextCursor: &repository.TicketCursor{CreatedAt: 1000, ID: 101},
	}, nil
}

func TestTicketHandlerListUsesSnakeCaseAndFilters(t *testing.T) {
	e := echo.New()
	repo := &fakeTicketRepository{}
	req := httptest.NewRequest(http.MethodGet, "/tickets?status=closed&limit=10&cursor=20:8&search=help", nil)
	rec := httptest.NewRecorder()

	err := NewTicketHandler(repo).List(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("list returned error: %v", err)
	}
	if rec.Code != http.StatusOK {
		t.Fatalf("expected ok, got %d", rec.Code)
	}
	if repo.listFilter.Status != "closed" || repo.listFilter.Limit != 10 || repo.listFilter.Search != "help" {
		t.Fatalf("unexpected list filter: %+v", repo.listFilter)
	}
	if repo.listFilter.Cursor == nil || repo.listFilter.Cursor.CreatedAt != 20 || repo.listFilter.Cursor.ID != 8 {
		t.Fatalf("unexpected cursor: %+v", repo.listFilter.Cursor)
	}
	body := rec.Body.String()
	for _, expected := range []string{`"channel_id":"456"`, `"initiator_id":"42"`, `"close_reason":"Solved"`, `"closed_by_id":"99"`, `"merged_into":9`, `"next_cursor":"10:7"`} {
		if !strings.Contains(body, expected) {
			t.Fatalf("expected %s in response, got %s", expected, body)
		}
	}
	for _, unexpected := range []string{`"initiator":`, `"closed_by":`, `"author":`, `"avatar_url":`, "channelId", "closeReason"} {
		if strings.Contains(body, unexpected) {
			t.Fatalf("did not expect %s in ticket response, got %s", unexpected, body)
		}
	}
}

func TestTicketHandlerMessagesReturnsSnakeCaseMessageFields(t *testing.T) {
	e := echo.New()
	repo := &fakeTicketRepository{}
	req := httptest.NewRequest(http.MethodGet, "/tickets/7/messages?limit=5", nil)
	rec := httptest.NewRecorder()
	ctx := e.NewContext(req, rec)
	ctx.SetParamNames("ticketID")
	ctx.SetParamValues("7")

	err := NewTicketHandler(repo).Messages(ctx)

	if err != nil {
		t.Fatalf("messages returned error: %v", err)
	}
	if repo.messageFilter.Limit != 5 {
		t.Fatalf("expected limit filter, got %+v", repo.messageFilter)
	}
	body := rec.Body.String()
	for _, expected := range []string{`"message_id":"101"`, `"author_id":"42"`, `"message_reference_id":"100"`, `"sticker_id":"200"`, `"is_edited":true`, `"is_deleted":false`} {
		if !strings.Contains(body, expected) {
			t.Fatalf("expected %s in response, got %s", expected, body)
		}
	}
	for _, unexpected := range []string{`"author":`, `"deleted_by":`, `"avatar_url":`, "messageReferenceId", "isEdited"} {
		if strings.Contains(body, unexpected) {
			t.Fatalf("did not expect %s in message response, got %s", unexpected, body)
		}
	}
}

func TestTicketHandlerMessagesMapsMissingTicketToNotFound(t *testing.T) {
	e := echo.New()
	req := httptest.NewRequest(http.MethodGet, "/tickets/7/messages", nil)
	rec := httptest.NewRecorder()
	ctx := e.NewContext(req, rec)
	ctx.SetParamNames("ticketID")
	ctx.SetParamValues("7")

	err := NewTicketHandler(&fakeTicketRepository{findErr: repository.ErrTicketNotFound}).Messages(ctx)

	if err != nil {
		t.Fatalf("messages returned error: %v", err)
	}
	if rec.Code != http.StatusNotFound {
		t.Fatalf("expected not found, got %d", rec.Code)
	}
}

func stringPtr(value string) *string {
	return &value
}

func intPtr(value int) *int {
	return &value
}
