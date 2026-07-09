package repository

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/go-sql-driver/mysql"
	"oficina-img/internal/domain/entity"
)

const DateOnlyLayout = "2006-01-02"

var (
	ErrDuplicateBirthday = errors.New("birthday already exists")
	ErrBirthdayNotFound  = errors.New("birthday not found")
)

type BirthdayFilter struct {
	Search string
	Month  int
}

type BirthdayRepository struct {
	db *sql.DB
}

func NewBirthdayRepository(db *sql.DB) *BirthdayRepository {
	return &BirthdayRepository{db: db}
}

func (r *BirthdayRepository) List(ctx context.Context, filter BirthdayFilter) ([]entity.Birthday, error) {
	query := strings.Builder{}
	query.WriteString("SELECT user_id, name, birthday, zone_hours, created_at, updated_at FROM birthdays WHERE 1 = 1")
	args := make([]any, 0, 4)

	if search := strings.TrimSpace(filter.Search); search != "" {
		needle := "%" + strings.ToLower(search) + "%"
		query.WriteString(" AND (LOWER(name) LIKE ? OR CAST(user_id AS CHAR) LIKE ?)")
		args = append(args, needle, "%"+search+"%")
	}

	if filter.Month > 0 {
		query.WriteString(" AND MONTH(birthday) = ?")
		args = append(args, filter.Month)
	}

	query.WriteString(" ORDER BY MONTH(birthday), DAYOFMONTH(birthday), LOWER(name), user_id")

	rows, err := r.db.QueryContext(ctx, query.String(), args...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var birthdays []entity.Birthday
	for rows.Next() {
		birthday, err := scanBirthday(rows)
		if err != nil {
			return nil, err
		}
		birthdays = append(birthdays, birthday)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}
	return birthdays, nil
}

func (r *BirthdayRepository) Create(ctx context.Context, birthday entity.Birthday) (entity.Birthday, error) {
	now := time.Now().Unix()
	birthday.CreatedAt = now
	birthday.UpdatedAt = now

	_, err := r.db.ExecContext(
		ctx,
		"INSERT INTO birthdays (user_id, name, birthday, zone_hours, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
		birthday.UserID,
		birthday.Name,
		birthday.Birthday.Format(DateOnlyLayout),
		birthday.ZoneHours,
		birthday.CreatedAt,
		birthday.UpdatedAt,
	)
	if err != nil {
		if isDuplicateKey(err) {
			return entity.Birthday{}, ErrDuplicateBirthday
		}
		return entity.Birthday{}, err
	}
	return birthday, nil
}

func (r *BirthdayRepository) Update(ctx context.Context, birthday entity.Birthday) (entity.Birthday, error) {
	birthday.UpdatedAt = time.Now().Unix()

	result, err := r.db.ExecContext(
		ctx,
		"UPDATE birthdays SET name = ?, birthday = ?, zone_hours = ?, updated_at = ? WHERE user_id = ?",
		birthday.Name,
		birthday.Birthday.Format(DateOnlyLayout),
		birthday.ZoneHours,
		birthday.UpdatedAt,
		birthday.UserID,
	)
	if err != nil {
		return entity.Birthday{}, err
	}

	affected, err := result.RowsAffected()
	if err != nil {
		return entity.Birthday{}, err
	}
	if affected == 0 {
		return entity.Birthday{}, ErrBirthdayNotFound
	}
	return birthday, nil
}

func (r *BirthdayRepository) Delete(ctx context.Context, userID int64) error {
	result, err := r.db.ExecContext(ctx, "DELETE FROM birthdays WHERE user_id = ?", userID)
	if err != nil {
		return err
	}

	affected, err := result.RowsAffected()
	if err != nil {
		return err
	}
	if affected == 0 {
		return ErrBirthdayNotFound
	}
	return nil
}

type birthdayScanner interface {
	Scan(dest ...any) error
}

func scanBirthday(scanner birthdayScanner) (entity.Birthday, error) {
	var birthday entity.Birthday
	var birthdayDate time.Time
	if err := scanner.Scan(
		&birthday.UserID,
		&birthday.Name,
		&birthdayDate,
		&birthday.ZoneHours,
		&birthday.CreatedAt,
		&birthday.UpdatedAt,
	); err != nil {
		return entity.Birthday{}, err
	}
	birthday.Birthday = normalizeDate(birthdayDate)
	return birthday, nil
}

func normalizeDate(value time.Time) time.Time {
	year, month, day := value.Date()
	return time.Date(year, month, day, 0, 0, 0, 0, time.UTC)
}

func isDuplicateKey(err error) bool {
	var mysqlErr *mysql.MySQLError
	return errors.As(err, &mysqlErr) && mysqlErr.Number == 1062
}

func ParseBirthdayDate(value string) (time.Time, error) {
	parsed, err := time.Parse(DateOnlyLayout, value)
	if err != nil {
		return time.Time{}, fmt.Errorf("birthday must use YYYY-MM-DD format")
	}
	return normalizeDate(parsed), nil
}
