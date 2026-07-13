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

type fakeMessageRepository struct {
	channelID int64
	filter    repository.MessageFilter
	err       error
}

func (f *fakeMessageRepository) List(_ context.Context, channelID int64, filter repository.MessageFilter) (repository.MessagePage, error) {
	f.channelID = channelID
	f.filter = filter
	if f.err != nil {
		return repository.MessagePage{}, f.err
	}
	ref, sticker := int64(100), int64(200)
	return repository.MessagePage{
		Messages: []entity.Message{{
			MessageID: 101, AuthorID: 42, MessageReferenceID: &ref,
			Content: stringPtr("hello"), StickerID: &sticker, IsEdited: true,
			RevisionCount: 2, CreatedAt: 1000, UpdatedAt: 1001,
		}},
		HasMoreBefore: true,
	}, nil
}

func (f *fakeMessageRepository) ListVersions(_ context.Context, _, _ int64) ([]entity.MessageVersion, error) {
	if f.err != nil {
		return nil, f.err
	}
	return []entity.MessageVersion{{MessageID: 101, AuthorID: 42, Content: stringPtr("original hello"), CreatedAt: 1000}}, nil
}

func TestChannelMessageHandlerListsChronologicalChannelMessages(t *testing.T) {
	e := echo.New()
	repo := &fakeMessageRepository{}
	req := httptest.NewRequest(http.MethodGet, "/channels/456/messages?limit=5&before=102", nil)
	rec := httptest.NewRecorder()
	ctx := e.NewContext(req, rec)
	ctx.SetParamNames("channelID")
	ctx.SetParamValues("456")

	if err := NewChannelMessageHandler(repo).List(ctx); err != nil {
		t.Fatalf("list returned error: %v", err)
	}
	if repo.channelID != 456 || repo.filter.Limit != 5 || repo.filter.BeforeID == nil || *repo.filter.BeforeID != 102 {
		t.Fatalf("unexpected channel message filter: channel=%d filter=%+v", repo.channelID, repo.filter)
	}
	for _, expected := range []string{`"channel_id":"456"`, `"message_id":"101"`, `"message_reference_id":"100"`, `"sticker_id":"200"`, `"has_more_before":true`, `"has_more_after":false`} {
		if !strings.Contains(rec.Body.String(), expected) {
			t.Fatalf("expected %s in response, got %s", expected, rec.Body.String())
		}
	}
}

func TestChannelMessageHandlerRejectsConflictingAnchors(t *testing.T) {
	e := echo.New()
	req := httptest.NewRequest(http.MethodGet, "/channels/456/messages?before=100&around=101", nil)
	rec := httptest.NewRecorder()
	ctx := e.NewContext(req, rec)
	ctx.SetParamNames("channelID")
	ctx.SetParamValues("456")

	if err := NewChannelMessageHandler(&fakeMessageRepository{}).List(ctx); err != nil {
		t.Fatalf("list returned error: %v", err)
	}
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("expected bad request, got %d", rec.Code)
	}
}

func TestChannelMessageHandlerMapsUnknownAroundMessageToNotFound(t *testing.T) {
	e := echo.New()
	req := httptest.NewRequest(http.MethodGet, "/channels/456/messages?around=101", nil)
	rec := httptest.NewRecorder()
	ctx := e.NewContext(req, rec)
	ctx.SetParamNames("channelID")
	ctx.SetParamValues("456")

	if err := NewChannelMessageHandler(&fakeMessageRepository{err: repository.ErrMessageNotFound}).List(ctx); err != nil {
		t.Fatalf("list returned error: %v", err)
	}
	if rec.Code != http.StatusNotFound {
		t.Fatalf("expected not found, got %d", rec.Code)
	}
}

func TestChannelMessageHandlerReturnsGenericVersionHistory(t *testing.T) {
	e := echo.New()
	req := httptest.NewRequest(http.MethodGet, "/channels/456/messages/101/versions", nil)
	rec := httptest.NewRecorder()
	ctx := e.NewContext(req, rec)
	ctx.SetParamNames("channelID", "messageID")
	ctx.SetParamValues("456", "101")

	if err := NewChannelMessageHandler(&fakeMessageRepository{}).Versions(ctx); err != nil {
		t.Fatalf("versions returned error: %v", err)
	}
	for _, expected := range []string{`"message_id":"101"`, `"content":"original hello"`, `"created_at":"1970-01-01T00:16:40Z"`} {
		if !strings.Contains(rec.Body.String(), expected) {
			t.Fatalf("expected %s in response, got %s", expected, rec.Body.String())
		}
	}
}
