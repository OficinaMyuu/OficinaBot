package store

import (
	"context"
	"database/sql"
	"fmt"
	"time"

	"oficina-registrar/internal/registration"
)

const insertRegisterSQL = `
INSERT INTO registers (target_id, moderator_id, age, gender, device, created_at)
VALUES (?, ?, ?, ?, ?, ?)
`

type RegisterRecord struct {
	TargetID    int64
	ModeratorID int64
	Age         int
	Gender      registration.Gender
	Device      registration.Device
}

type RegisterRepository struct {
	db  *sql.DB
	now func() time.Time
}

func NewRegisterRepository(db *sql.DB, now func() time.Time) *RegisterRepository {
	if now == nil {
		now = time.Now
	}
	return &RegisterRepository{db: db, now: now}
}

func (r *RegisterRepository) Save(ctx context.Context, record RegisterRecord) error {
	_, err := r.db.ExecContext(
		ctx,
		insertRegisterSQL,
		record.TargetID,
		record.ModeratorID,
		record.Age,
		string(record.Gender),
		string(record.Device),
		r.now().UnixMilli(),
	)
	if err != nil {
		return fmt.Errorf("insert register: %w", err)
	}
	return nil
}
