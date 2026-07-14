package repository

import (
	"context"
	"testing"
)

func TestUserRepositoryIntegrationFindManyReturnsBotFlag(t *testing.T) {
	db := openTemporaryMySQLSchema(t, testMySQLDSN(t))
	if _, err := db.Exec(`CREATE TABLE users (
		id BIGINT PRIMARY KEY,
		name VARCHAR(255) NOT NULL,
		global_name VARCHAR(255),
		avatar_hash VARCHAR(128),
		is_bot BOOLEAN NOT NULL DEFAULT FALSE,
		created_at BIGINT NOT NULL,
		updated_at BIGINT NOT NULL
	)`); err != nil {
		t.Fatalf("create users table: %v", err)
	}
	if _, err := db.Exec(`INSERT INTO users
		(id, name, global_name, avatar_hash, is_bot, created_at, updated_at)
		VALUES (42, 'myuu', 'Oficina Myuu', 'avatar-hash', TRUE, 1, 1)`); err != nil {
		t.Fatalf("insert user fixture: %v", err)
	}

	users, err := NewUserRepository(db).FindMany(context.Background(), []int64{42})
	if err != nil {
		t.Fatalf("find users: %v", err)
	}
	if len(users) != 1 || !users[0].IsBot {
		t.Fatalf("expected bot user, got %+v", users)
	}
}
