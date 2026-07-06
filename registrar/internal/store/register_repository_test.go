package store

import (
	"context"
	"regexp"
	"testing"
	"time"

	"github.com/DATA-DOG/go-sqlmock"

	"oficina-registrar/internal/registration"
)

func TestRegisterRepositorySaveInsertsRegisterRow(t *testing.T) {
	db, mock, err := sqlmock.New()
	if err != nil {
		t.Fatalf("sqlmock.New() error = %v", err)
	}
	defer db.Close()

	now := time.Date(2026, 7, 5, 12, 30, 0, 123_000_000, time.UTC)
	repository := NewRegisterRepository(db, func() time.Time { return now })

	mock.ExpectExec(regexp.QuoteMeta(insertRegisterSQL)).
		WithArgs(int64(123), int64(456), 18, "FEMALE", "DESKTOP", now.UnixMilli()).
		WillReturnResult(sqlmock.NewResult(1, 1))

	err = repository.Save(context.Background(), RegisterRecord{
		TargetID:    123,
		ModeratorID: 456,
		Age:         18,
		Gender:      registration.GenderFemale,
		Device:      registration.DeviceDesktop,
	})
	if err != nil {
		t.Fatalf("Save() error = %v", err)
	}

	if err := mock.ExpectationsWereMet(); err != nil {
		t.Fatalf("unmet sql expectations: %v", err)
	}
}
