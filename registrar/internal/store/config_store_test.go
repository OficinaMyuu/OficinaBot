package store

import (
	"context"
	"errors"
	"regexp"
	"testing"

	"github.com/DATA-DOG/go-sqlmock"
)

func TestConfigStoreGetQuotesReservedKeyColumnAndCachesValue(t *testing.T) {
	db, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("sqlmock.New() error = %v", err)
	}
	defer db.Close()

	mock.ExpectQuery(regexp.QuoteMeta(selectConfigValueSQL)).
		WithArgs("app.token").
		WillReturnRows(sqlmock.NewRows([]string{"value"}).AddRow("token-value"))

	store := NewConfigStore(db)
	got, err := store.Get(context.Background(), "app.token")
	if err != nil {
		t.Fatalf("Get() error = %v", err)
	}
	if got != "token-value" {
		t.Fatalf("Get() = %q, want token-value", got)
	}

	got, err = store.Get(context.Background(), "app.token")
	if err != nil {
		t.Fatalf("cached Get() error = %v", err)
	}
	if got != "token-value" {
		t.Fatalf("cached Get() = %q, want token-value", got)
	}

	if err := mock.ExpectationsWereMet(); err != nil {
		t.Fatalf("unmet sql expectations: %v", err)
	}
}

func TestConfigStoreGetReportsMissingConfig(t *testing.T) {
	db, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("sqlmock.New() error = %v", err)
	}
	defer db.Close()

	mock.ExpectQuery(regexp.QuoteMeta(selectConfigValueSQL)).
		WithArgs("missing.key").
		WillReturnRows(sqlmock.NewRows([]string{"value"}))

	store := NewConfigStore(db)
	_, err = store.Get(context.Background(), "missing.key")
	if !errors.Is(err, ErrConfigNotFound) {
		t.Fatalf("Get() error = %v, want ErrConfigNotFound", err)
	}
}
