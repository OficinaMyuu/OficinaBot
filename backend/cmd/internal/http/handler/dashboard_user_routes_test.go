package handler

import (
	"context"
	"net/http"
	"net/http/httptest"
	"reflect"
	"strings"
	"testing"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/domain/entity"
)

type fakeUserRepository struct {
	userIDs []int64
	users   []entity.User
	err     error
}

func (f *fakeUserRepository) FindMany(_ context.Context, userIDs []int64) ([]entity.User, error) {
	f.userIDs = userIDs
	return f.users, f.err
}

func TestUserHandlerQueryDedupesIDsAndReturnsAvatarHash(t *testing.T) {
	e := echo.New()
	hash := "avatar-hash"
	repo := &fakeUserRepository{
		users: []entity.User{{
			ID:         42,
			Username:   textPtr("myuu"),
			GlobalName: textPtr("Oficina Myuu"),
			AvatarHash: &hash,
		}},
	}
	req := httptest.NewRequest(http.MethodPost, "/users/query", strings.NewReader(`{"user_ids":["42","42","99"]}`))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()

	err := NewUserHandler(repo).Query(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("query returned error: %v", err)
	}
	if rec.Code != http.StatusOK {
		t.Fatalf("expected ok, got %d", rec.Code)
	}
	if !reflect.DeepEqual(repo.userIDs, []int64{42, 99}) {
		t.Fatalf("expected deduped ids, got %+v", repo.userIDs)
	}
	body := rec.Body.String()
	for _, expected := range []string{`"id":"42"`, `"display_name":"Oficina Myuu"`, `"avatar_hash":"avatar-hash"`, `"avatar_url":"https://cdn.discordapp.com/avatars/42/avatar-hash.png"`} {
		if !strings.Contains(body, expected) {
			t.Fatalf("expected %s in response, got %s", expected, body)
		}
	}
}

func TestUserHandlerQueryRejectsInvalidID(t *testing.T) {
	e := echo.New()
	req := httptest.NewRequest(http.MethodPost, "/users/query", strings.NewReader(`{"user_ids":["abc"]}`))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()

	err := NewUserHandler(&fakeUserRepository{}).Query(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("query returned error: %v", err)
	}
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("expected bad request, got %d", rec.Code)
	}
}

func textPtr(value string) *string {
	return &value
}
