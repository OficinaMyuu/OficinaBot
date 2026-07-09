package repository

import (
	"context"
	"database/sql"
	"errors"

	"oficina-img/internal/domain/entity"
)

var ErrSessionNotFound = errors.New("dashboard session not found")

type DashboardSessionRepository struct {
	db *sql.DB
}

func NewDashboardSessionRepository(db *sql.DB) *DashboardSessionRepository {
	return &DashboardSessionRepository{db: db}
}

func (r *DashboardSessionRepository) Save(ctx context.Context, sessionIDHash string, session entity.DashboardSession) error {
	_, err := r.db.ExecContext(ctx, `
INSERT INTO dashboard_sessions (
	session_id_hash, csrf_token, user_id, username, global_name, avatar_url,
	guild_name, guild_icon_url, permissions, expires_at, created_at, updated_at
) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, UNIX_TIMESTAMP(), UNIX_TIMESTAMP())
ON DUPLICATE KEY UPDATE
	csrf_token = VALUES(csrf_token),
	user_id = VALUES(user_id),
	username = VALUES(username),
	global_name = VALUES(global_name),
	avatar_url = VALUES(avatar_url),
	guild_name = VALUES(guild_name),
	guild_icon_url = VALUES(guild_icon_url),
	permissions = VALUES(permissions),
	expires_at = VALUES(expires_at),
	updated_at = UNIX_TIMESTAMP()`,
		sessionIDHash,
		session.CSRFToken,
		session.User.ID,
		session.User.Username,
		session.User.GlobalName,
		session.User.AvatarURL,
		session.User.GuildName,
		session.User.GuildIconURL,
		session.User.Permissions,
		session.ExpiresAt,
	)
	return err
}

func (r *DashboardSessionRepository) Find(ctx context.Context, sessionIDHash string) (entity.DashboardSession, error) {
	var session entity.DashboardSession
	var globalName, avatarURL, guildIconURL sql.NullString
	err := r.db.QueryRowContext(ctx, `
SELECT csrf_token, user_id, username, global_name, avatar_url, guild_name, guild_icon_url, permissions, expires_at
FROM dashboard_sessions
WHERE session_id_hash = ?
LIMIT 1`, sessionIDHash).Scan(
		&session.CSRFToken,
		&session.User.ID,
		&session.User.Username,
		&globalName,
		&avatarURL,
		&session.User.GuildName,
		&guildIconURL,
		&session.User.Permissions,
		&session.ExpiresAt,
	)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return entity.DashboardSession{}, ErrSessionNotFound
		}
		return entity.DashboardSession{}, err
	}
	session.User.GlobalName = nullableString(globalName)
	session.User.AvatarURL = nullableString(avatarURL)
	session.User.GuildIconURL = nullableString(guildIconURL)
	return session, nil
}

func (r *DashboardSessionRepository) Delete(ctx context.Context, sessionIDHash string) error {
	_, err := r.db.ExecContext(ctx, "DELETE FROM dashboard_sessions WHERE session_id_hash = ?", sessionIDHash)
	return err
}

func (r *DashboardSessionRepository) DeleteExpired(ctx context.Context, now int64) error {
	_, err := r.db.ExecContext(ctx, "DELETE FROM dashboard_sessions WHERE expires_at <= ?", now)
	return err
}
