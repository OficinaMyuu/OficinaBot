package repository

import (
	"context"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/go-sql-driver/mysql"
	"gorm.io/gorm"
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
	db *gorm.DB
}

func NewBirthdayRepository(db *gorm.DB) *BirthdayRepository {
	return &BirthdayRepository{db: db}
}

func (r *BirthdayRepository) List(ctx context.Context, filter BirthdayFilter) ([]entity.Birthday, error) {
	query := r.db.WithContext(ctx).Model(&birthdayRow{})

	if search := strings.TrimSpace(filter.Search); search != "" {
		needle := "%" + strings.ToLower(search) + "%"
		query = query.Where("LOWER(name) LIKE ? OR CAST(user_id AS CHAR) LIKE ?", needle, "%"+search+"%")
	}

	if filter.Month > 0 {
		query = query.Where("MONTH(birthday) = ?", filter.Month)
	}

	var rows []birthdayRow
	if err := query.Order("MONTH(birthday), DAYOFMONTH(birthday), LOWER(name), user_id").Find(&rows).Error; err != nil {
		return nil, err
	}
	birthdays := make([]entity.Birthday, 0, len(rows))
	for _, row := range rows {
		birthdays = append(birthdays, row.entity())
	}
	return birthdays, nil
}

func (r *BirthdayRepository) Create(ctx context.Context, birthday entity.Birthday) (entity.Birthday, error) {
	now := time.Now().Unix()
	birthday.CreatedAt = now
	birthday.UpdatedAt = now

	row := birthdayRow{
		UserID: birthday.UserID, Name: birthday.Name, Birthday: normalizeDate(birthday.Birthday),
		ZoneHours: birthday.ZoneHours, CreatedAt: birthday.CreatedAt, UpdatedAt: birthday.UpdatedAt,
	}
	err := r.db.WithContext(ctx).Create(&row).Error
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

	result := r.db.WithContext(ctx).Model(&birthdayRow{}).
		Where("user_id = ?", birthday.UserID).
		Updates(map[string]any{
			"name": birthday.Name, "birthday": normalizeDate(birthday.Birthday),
			"zone_hours": birthday.ZoneHours, "updated_at": birthday.UpdatedAt,
		})
	if result.Error != nil {
		return entity.Birthday{}, result.Error
	}
	if result.RowsAffected == 0 {
		return entity.Birthday{}, ErrBirthdayNotFound
	}
	return birthday, nil
}

func (r *BirthdayRepository) Delete(ctx context.Context, userID int64) error {
	result := r.db.WithContext(ctx).Where("user_id = ?", userID).Delete(&birthdayRow{})
	if result.Error != nil {
		return result.Error
	}
	if result.RowsAffected == 0 {
		return ErrBirthdayNotFound
	}
	return nil
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
