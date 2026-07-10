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

	"github.com/labstack/echo/v4"
	"oficina-img/internal/contract"
	"oficina-img/internal/domain/entity"
	"oficina-img/internal/domain/mysql/repository"
)

type fakeStoreItemSettingsRepository struct {
	items        []entity.StoreItemSetting
	listErr      error
	updateErr    error
	updatedItem  string
	updatedPrice int
	updatedBy    int64
}

func (f *fakeStoreItemSettingsRepository) List(_ context.Context) ([]entity.StoreItemSetting, error) {
	return f.items, f.listErr
}

func (f *fakeStoreItemSettingsRepository) Update(
	_ context.Context,
	itemType string,
	price int,
	updatedBy int64,
) (entity.StoreItemSetting, error) {
	f.updatedItem = itemType
	f.updatedPrice = price
	f.updatedBy = updatedBy
	if f.updateErr != nil {
		return entity.StoreItemSetting{}, f.updateErr
	}
	return entity.StoreItemSetting{
		ItemType:  itemType,
		Price:     price,
		CreatedAt: 1,
		UpdatedAt: 1_700_000_000_123,
		UpdatedBy: &updatedBy,
	}, nil
}

func TestStoreItemSettingsHandlerListsRFC3339Timestamps(t *testing.T) {
	e := echo.New()
	handler := NewStoreItemSettingsHandler(&fakeStoreItemSettingsRepository{items: []entity.StoreItemSetting{{
		ItemType:  "GROUP",
		Price:     600000,
		CreatedAt: 1,
		UpdatedAt: 1_700_000_000_123,
	}}})
	req := httptest.NewRequest(http.MethodGet, "/economy/action-costs", nil)
	rec := httptest.NewRecorder()

	if err := handler.List(e.NewContext(req, rec)); err != nil {
		t.Fatalf("list returned error: %v", err)
	}
	if rec.Code != http.StatusOK {
		t.Fatalf("expected ok, got %d", rec.Code)
	}
	for _, expected := range []string{
		`"item_type":"GROUP"`,
		`"created_at":"1970-01-01T00:00:01Z"`,
		`"updated_at":"2023-11-14T22:13:20Z"`,
	} {
		if !strings.Contains(rec.Body.String(), expected) {
			t.Fatalf("expected %s in response, got %s", expected, rec.Body.String())
		}
	}
}

func TestStoreItemSettingsHandlerUpdatesWithSessionActor(t *testing.T) {
	e := echo.New()
	repository := &fakeStoreItemSettingsRepository{}
	handler := NewStoreItemSettingsHandler(repository)
	body := storeItemSettingsJSON(t, contract.StoreItemSettingsUpdateRequest{Price: 0})
	req := httptest.NewRequest(http.MethodPatch, "/economy/action-costs/GROUP", bytes.NewReader(body))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()
	ctx := e.NewContext(req, rec)
	ctx.SetParamNames("itemType")
	ctx.SetParamValues("GROUP")
	ctx.Set("dashboardSession", DashboardSession{User: DashboardUser{ID: "42"}})

	if err := handler.Update(ctx); err != nil {
		t.Fatalf("update returned error: %v", err)
	}
	if rec.Code != http.StatusOK {
		t.Fatalf("expected ok, got %d", rec.Code)
	}
	if repository.updatedItem != "GROUP" || repository.updatedPrice != 0 || repository.updatedBy != 42 {
		t.Fatalf("unexpected update call: %+v", repository)
	}
	if !strings.Contains(rec.Body.String(), `"updated_by":"42"`) {
		t.Fatalf("expected serialized updater, got %s", rec.Body.String())
	}
}

func TestStoreItemSettingsHandlerRejectsNegativePrice(t *testing.T) {
	e := echo.New()
	repository := &fakeStoreItemSettingsRepository{}
	handler := NewStoreItemSettingsHandler(repository)
	body := storeItemSettingsJSON(t, contract.StoreItemSettingsUpdateRequest{Price: -1})
	req := httptest.NewRequest(http.MethodPatch, "/economy/action-costs/GROUP", bytes.NewReader(body))
	req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	rec := httptest.NewRecorder()
	ctx := e.NewContext(req, rec)
	ctx.SetParamNames("itemType")
	ctx.SetParamValues("GROUP")
	ctx.Set("dashboardSession", DashboardSession{User: DashboardUser{ID: "42"}})

	if err := handler.Update(ctx); err != nil {
		t.Fatalf("update returned error: %v", err)
	}
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("expected bad request, got %d", rec.Code)
	}
	if repository.updatedItem != "" {
		t.Fatalf("repository must not be called for a negative price")
	}
}

func TestStoreItemSettingsHandlerMapsUpdateErrors(t *testing.T) {
	tests := []struct {
		name string
		err  error
		want int
	}{
		{name: "not found", err: repository.ErrStoreItemSettingNotFound, want: http.StatusNotFound},
		{name: "unexpected", err: errors.New("boom"), want: http.StatusInternalServerError},
	}

	for _, test := range tests {
		t.Run(test.name, func(t *testing.T) {
			e := echo.New()
			handler := NewStoreItemSettingsHandler(&fakeStoreItemSettingsRepository{updateErr: test.err})
			body := storeItemSettingsJSON(t, contract.StoreItemSettingsUpdateRequest{Price: 1})
			req := httptest.NewRequest(http.MethodPatch, "/economy/action-costs/GROUP", bytes.NewReader(body))
			req.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
			rec := httptest.NewRecorder()
			ctx := e.NewContext(req, rec)
			ctx.SetParamNames("itemType")
			ctx.SetParamValues("GROUP")
			ctx.Set("dashboardSession", DashboardSession{User: DashboardUser{ID: "42"}})

			if err := handler.Update(ctx); err != nil {
				t.Fatalf("update returned error: %v", err)
			}
			if rec.Code != test.want {
				t.Fatalf("expected %d, got %d", test.want, rec.Code)
			}
		})
	}
}

func TestStoreItemSettingsRoutesRequireSessionAndCSRF(t *testing.T) {
	e := echo.New()
	sessions := NewSessionStore()
	session, err := sessions.Create(DashboardUser{ID: "42"})
	if err != nil {
		t.Fatalf("create session: %v", err)
	}

	RegisterDashboardRoutes(e, DashboardRoutesConfig{
		AuthConfig:  testAuthConfig(nil),
		OAuthClient: stubDiscordOAuthClient{},
		Sessions:    sessions,
		StoreItems:  &fakeStoreItemSettingsRepository{},
	})

	unauthenticated := httptest.NewRequest(http.MethodGet, "/economy/action-costs", nil)
	unauthenticatedRecorder := httptest.NewRecorder()
	e.ServeHTTP(unauthenticatedRecorder, unauthenticated)
	if unauthenticatedRecorder.Code != http.StatusUnauthorized {
		t.Fatalf("expected unauthenticated list to fail, got %d", unauthenticatedRecorder.Code)
	}

	body := storeItemSettingsJSON(t, contract.StoreItemSettingsUpdateRequest{Price: 1})
	missingCSRF := httptest.NewRequest(http.MethodPatch, "/economy/action-costs/GROUP", bytes.NewReader(body))
	missingCSRF.Header.Set(echo.HeaderContentType, echo.MIMEApplicationJSON)
	missingCSRF.AddCookie(&http.Cookie{Name: dashboardSessionCookie, Value: session.ID})
	missingCSRFRecorder := httptest.NewRecorder()
	e.ServeHTTP(missingCSRFRecorder, missingCSRF)
	if missingCSRFRecorder.Code != http.StatusForbidden {
		t.Fatalf("expected mutation without CSRF token to fail, got %d", missingCSRFRecorder.Code)
	}
}

func storeItemSettingsJSON(t *testing.T, req contract.StoreItemSettingsUpdateRequest) []byte {
	t.Helper()
	body, err := json.Marshal(req)
	if err != nil {
		t.Fatalf("marshal action cost request: %v", err)
	}
	return body
}
