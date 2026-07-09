package routes

import (
	"context"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/store"
)

type fakeTicketRepository struct {
	listFilter      store.TicketListFilter
	messageFilter   store.TicketMessageFilter
	findErr         error
	listErr         error
	listMessagesErr error
}

func (f *fakeTicketRepository) List(_ context.Context, filter store.TicketListFilter) (store.TicketPage, error) {
	f.listFilter = filter
	if f.listErr != nil {
		return store.TicketPage{}, f.listErr
	}
	closedBy := store.TicketUser{ID: 99, Username: stringPtr("staff"), GlobalName: stringPtr("Staff")}
	return store.TicketPage{
		Tickets: []store.Ticket{{
			ID:          7,
			Title:       "Need help",
			Description: "The thing exploded",
			GuildID:     123,
			ChannelID:   456,
			Initiator:   store.TicketUser{ID: 42, Username: stringPtr("myuu"), GlobalName: stringPtr("Myuu")},
			CloseReason: stringPtr("Solved"),
			ClosedBy:    &closedBy,
			MergedInto:  intPtr(9),
			CreatedAt:   10,
			UpdatedAt:   11,
		}},
		NextCursor: &store.TicketCursor{CreatedAt: 10, ID: 7},
	}, nil
}

func (f *fakeTicketRepository) Find(_ context.Context, _ int) (store.Ticket, error) {
	if f.findErr != nil {
		return store.Ticket{}, f.findErr
	}
	return store.Ticket{
		ID:          7,
		Title:       "Need help",
		Description: "The thing exploded",
		GuildID:     123,
		ChannelID:   456,
		Initiator:   store.TicketUser{ID: 42, Username: stringPtr("myuu")},
		CreatedAt:   10,
		UpdatedAt:   11,
	}, nil
}

func (f *fakeTicketRepository) ListMessages(_ context.Context, _ int64, filter store.TicketMessageFilter) (store.TicketMessagePage, error) {
	f.messageFilter = filter
	if f.listMessagesErr != nil {
		return store.TicketMessagePage{}, f.listMessagesErr
	}
	ref := int64(100)
	sticker := int64(200)
	return store.TicketMessagePage{
		Messages: []store.TicketMessage{{
			MessageID:          101,
			Author:             store.TicketUser{ID: 42, Username: stringPtr("myuu"), GlobalName: stringPtr("Myuu")},
			MessageReferenceID: &ref,
			Content:            stringPtr("hello"),
			StickerID:          &sticker,
			IsEdited:           true,
			CreatedAt:          1000,
			UpdatedAt:          1001,
		}},
		NextCursor: &store.TicketCursor{CreatedAt: 1000, ID: 101},
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
	for _, expected := range []string{`"channel_id":"456"`, `"avatar_url":"https://cdn.discordapp.com/embed/avatars/`, `"close_reason":"Solved"`, `"closed_by":`, `"merged_into":9`, `"next_cursor":"10:7"`} {
		if !strings.Contains(body, expected) {
			t.Fatalf("expected %s in response, got %s", expected, body)
		}
	}
	if strings.Contains(body, "channelId") || strings.Contains(body, "closeReason") {
		t.Fatalf("expected snake_case ticket response, got %s", body)
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
	for _, expected := range []string{`"message_id":"101"`, `"avatar_url":"https://cdn.discordapp.com/embed/avatars/`, `"message_reference_id":"100"`, `"sticker_id":"200"`, `"is_edited":true`, `"is_deleted":false`} {
		if !strings.Contains(body, expected) {
			t.Fatalf("expected %s in response, got %s", expected, body)
		}
	}
	if strings.Contains(body, "messageReferenceId") || strings.Contains(body, "isEdited") {
		t.Fatalf("expected snake_case message response, got %s", body)
	}
}

func TestTicketHandlerMessagesMapsMissingTicketToNotFound(t *testing.T) {
	e := echo.New()
	req := httptest.NewRequest(http.MethodGet, "/tickets/7/messages", nil)
	rec := httptest.NewRecorder()
	ctx := e.NewContext(req, rec)
	ctx.SetParamNames("ticketID")
	ctx.SetParamValues("7")

	err := NewTicketHandler(&fakeTicketRepository{findErr: store.ErrTicketNotFound}).Messages(ctx)

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
