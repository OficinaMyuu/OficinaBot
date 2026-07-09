package handler

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/contract"
	"oficina-img/internal/domain/entity"
	"oficina-img/internal/domain/mysql/repository"
)

type fakeBirthdayRepository struct {
	listFilter repository.BirthdayFilter
	createErr  error
	updateErr  error
	deleteErr  error
}

func (f *fakeBirthdayRepository) List(_ context.Context, filter repository.BirthdayFilter) ([]entity.Birthday, error) {
	f.listFilter = filter
	return []entity.Birthday{{
		UserID:    42,
		Name:      "Myuu",
		Birthday:  time.Date(2020, time.May, 10, 0, 0, 0, 0, time.UTC),
		ZoneHours: -3,
		CreatedAt: 1,
		UpdatedAt: 2,
	}}, nil
}

func (f *fakeBirthdayRepository) Create(_ context.Context, birthday entity.Birthday) (entity.Birthday, error) {
	if f.createErr != nil {
		return entity.Birthday{}, f.createErr
	}
	birthday.CreatedAt = 1
	birthday.UpdatedAt = 1
	return birthday, nil
}

func (f *fakeBirthdayRepository) Update(_ context.Context, birthday entity.Birthday) (entity.Birthday, error) {
	if f.updateErr != nil {
		return entity.Birthday{}, f.updateErr
	}
	birthday.UpdatedAt = 2
	return birthday, nil
}

func (f *fakeBirthdayRepository) Delete(_ context.Context, _ int64) error {
	return f.deleteErr
}

func TestBirthdayHandlerListAppliesMonthFilter(t *testing.T) {
	e := echo.New()
	repo := &fakeBirthdayRepository{}
	req := httptest.NewRequest(http.MethodGet, "/birthdays?month=5&search=myuu", nil)
	rec := httptest.NewRecorder()

	err := NewBirthdayHandler(repo).List(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("list returned error: %v", err)
	}
	if rec.Code != http.StatusOK {
		t.Fatalf("expected ok, got %d", rec.Code)
	}
	if repo.listFilter.Month != 5 || repo.listFilter.Search != "myuu" {
		t.Fatalf("unexpected filter: %+v", repo.listFilter)
	}
	if !strings.Contains(rec.Body.String(), `"user_id":"42"`) {
		t.Fatalf("expected user id response, got %s", rec.Body.String())
	}
}

func TestBirthdayHandlerRejectsInvalidCreatePayload(t *testing.T) {
	e := echo.New()
	req := httptest.NewRequest(http.MethodPost, "/birthdays", strings.NewReader(`{"user_id":"abc"}`))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()

	err := NewBirthdayHandler(&fakeBirthdayRepository{}).Create(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("create returned error: %v", err)
	}
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("expected bad request, got %d", rec.Code)
	}
}

func TestBirthdayHandlerMapsDuplicateCreateToConflict(t *testing.T) {
	e := echo.New()
	body := birthdayJSON(t, contract.BirthdayRequest{
		UserID:    "42",
		Name:      "Myuu",
		Birthday:  "2020-05-10",
		ZoneHours: -3,
	})
	req := httptest.NewRequest(http.MethodPost, "/birthdays", bytes.NewReader(body))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()

	err := NewBirthdayHandler(&fakeBirthdayRepository{createErr: repository.ErrDuplicateBirthday}).Create(e.NewContext(req, rec))

	if err != nil {
		t.Fatalf("create returned error: %v", err)
	}
	if rec.Code != http.StatusConflict {
		t.Fatalf("expected conflict, got %d", rec.Code)
	}
}

func TestBirthdayHandlerRejectsRoutePayloadMismatch(t *testing.T) {
	e := echo.New()
	body := birthdayJSON(t, contract.BirthdayRequest{
		UserID:    "43",
		Name:      "Myuu",
		Birthday:  "2020-05-10",
		ZoneHours: -3,
	})
	req := httptest.NewRequest(http.MethodPut, "/birthdays/42", bytes.NewReader(body))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()
	ctx := e.NewContext(req, rec)
	ctx.SetParamNames("userID")
	ctx.SetParamValues("42")

	err := NewBirthdayHandler(&fakeBirthdayRepository{}).Update(ctx)

	if err != nil {
		t.Fatalf("update returned error: %v", err)
	}
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("expected bad request, got %d", rec.Code)
	}
}

func TestBirthdayHandlerMapsMissingDeleteToNotFound(t *testing.T) {
	e := echo.New()
	req := httptest.NewRequest(http.MethodDelete, "/birthdays/42", nil)
	rec := httptest.NewRecorder()
	ctx := e.NewContext(req, rec)
	ctx.SetParamNames("userID")
	ctx.SetParamValues("42")

	err := NewBirthdayHandler(&fakeBirthdayRepository{deleteErr: repository.ErrBirthdayNotFound}).Delete(ctx)

	if err != nil {
		t.Fatalf("delete returned error: %v", err)
	}
	if rec.Code != http.StatusNotFound {
		t.Fatalf("expected not found, got %d", rec.Code)
	}
}

func TestBirthdayHandlerMapsUnexpectedDeleteError(t *testing.T) {
	e := echo.New()
	req := httptest.NewRequest(http.MethodDelete, "/birthdays/42", nil)
	rec := httptest.NewRecorder()
	ctx := e.NewContext(req, rec)
	ctx.SetParamNames("userID")
	ctx.SetParamValues("42")

	err := NewBirthdayHandler(&fakeBirthdayRepository{deleteErr: errors.New("boom")}).Delete(ctx)

	if err != nil {
		t.Fatalf("delete returned error: %v", err)
	}
	if rec.Code != http.StatusInternalServerError {
		t.Fatalf("expected internal error, got %d", rec.Code)
	}
}

func birthdayJSON(t *testing.T, req contract.BirthdayRequest) []byte {
	t.Helper()
	body, err := json.Marshal(req)
	if err != nil {
		t.Fatalf("marshal birthday request: %v", err)
	}
	return body
}
