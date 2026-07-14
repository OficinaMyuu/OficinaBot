package repository

import (
	"context"
	"errors"
	"testing"
)

func TestMessageRepositoryIntegrationVersionsAndAnchorValidation(t *testing.T) {
	db := openTemporaryMySQLSchema(t, testMySQLDSN(t))
	createTicketTables(t, db)
	insertTicketFixtures(t, db)
	repository := NewMessageRepository(openTestGORM(t, db))
	ctx := context.Background()

	versions, err := repository.ListVersions(ctx, 456, 100)
	if err != nil {
		t.Fatalf("list message versions: %v", err)
	}
	if len(versions) != 2 || versions[0].Content == nil || *versions[0].Content != "hello" || versions[1].Content == nil || *versions[1].Content != "edited hello" {
		t.Fatalf("unexpected message versions: %+v", versions)
	}

	versions, err = repository.ListVersions(ctx, 456, 101)
	if err != nil {
		t.Fatalf("list non-deletion versions: %v", err)
	}
	if len(versions) != 1 || versions[0].MessageReferenceID == nil || *versions[0].MessageReferenceID != 100 {
		t.Fatalf("expected only the original reply version, got %+v", versions)
	}

	if _, err := repository.ListVersions(ctx, 456, 999); !errors.Is(err, ErrMessageNotFound) {
		t.Fatalf("expected missing versions error, got %v", err)
	}
	anchor := int64(100)
	if _, err := repository.List(ctx, 456, MessageFilter{BeforeID: &anchor, AfterID: &anchor}); err == nil {
		t.Fatal("expected mutually exclusive anchor validation error")
	}
	missingAnchor := int64(999)
	if _, err := repository.List(ctx, 456, MessageFilter{BeforeID: &missingAnchor}); !errors.Is(err, ErrMessageNotFound) {
		t.Fatalf("expected missing anchor error, got %v", err)
	}
}
