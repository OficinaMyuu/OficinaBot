package repository

import (
	"context"
	"database/sql"
	"strings"

	"oficina-img/internal/domain/entity"
)

type UserRepository struct {
	db *sql.DB
}

func NewUserRepository(db *sql.DB) *UserRepository {
	return &UserRepository{db: db}
}

func (r *UserRepository) FindMany(ctx context.Context, userIDs []int64) ([]entity.User, error) {
	if len(userIDs) == 0 {
		return []entity.User{}, nil
	}

	args := make([]any, 0, len(userIDs))
	placeholders := make([]string, 0, len(userIDs))
	for _, userID := range userIDs {
		args = append(args, userID)
		placeholders = append(placeholders, "?")
	}

	rows, err := r.db.QueryContext(ctx, `
SELECT id, name, global_name, avatar_hash
FROM users
WHERE id IN (`+strings.Join(placeholders, ", ")+`)
ORDER BY name ASC, id ASC`, args...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	users := make([]entity.User, 0, len(userIDs))
	for rows.Next() {
		var user entity.User
		var username, globalName, avatarHash sql.NullString
		if err := rows.Scan(&user.ID, &username, &globalName, &avatarHash); err != nil {
			return nil, err
		}
		user.Username = nullableString(username)
		user.GlobalName = nullableString(globalName)
		user.AvatarHash = nullableString(avatarHash)
		users = append(users, user)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}
	return users, nil
}
