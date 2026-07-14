package repository

import (
	"time"

	"oficina-img/internal/domain/entity"
)

type birthdayRow struct {
	UserID    int64     `gorm:"column:user_id;primaryKey;autoIncrement:false"`
	Name      string    `gorm:"column:name"`
	Birthday  time.Time `gorm:"column:birthday"`
	ZoneHours int       `gorm:"column:zone_hours"`
	CreatedAt int64     `gorm:"column:created_at;autoCreateTime:false"`
	UpdatedAt int64     `gorm:"column:updated_at;autoUpdateTime:false"`
}

func (birthdayRow) TableName() string { return "birthdays" }

func (row birthdayRow) entity() entity.Birthday {
	return entity.Birthday{
		UserID:    row.UserID,
		Name:      row.Name,
		Birthday:  normalizeDate(row.Birthday),
		ZoneHours: row.ZoneHours,
		CreatedAt: row.CreatedAt,
		UpdatedAt: row.UpdatedAt,
	}
}

type dashboardSessionRow struct {
	SessionIDHash string  `gorm:"column:session_id_hash;primaryKey"`
	CSRFToken     string  `gorm:"column:csrf_token"`
	UserID        string  `gorm:"column:user_id"`
	Username      string  `gorm:"column:username"`
	GlobalName    *string `gorm:"column:global_name"`
	AvatarURL     *string `gorm:"column:avatar_url"`
	GuildName     string  `gorm:"column:guild_name"`
	GuildIconURL  *string `gorm:"column:guild_icon_url"`
	Permissions   string  `gorm:"column:permissions"`
	ExpiresAt     int64   `gorm:"column:expires_at"`
	CreatedAt     int64   `gorm:"column:created_at;autoCreateTime:false"`
	UpdatedAt     int64   `gorm:"column:updated_at;autoUpdateTime:false"`
}

func (dashboardSessionRow) TableName() string { return "dashboard_sessions" }

func (row dashboardSessionRow) entity() entity.DashboardSession {
	return entity.DashboardSession{
		User: entity.DashboardUser{
			ID:           row.UserID,
			Username:     row.Username,
			GlobalName:   row.GlobalName,
			AvatarURL:    row.AvatarURL,
			GuildName:    row.GuildName,
			GuildIconURL: row.GuildIconURL,
			Permissions:  row.Permissions,
		},
		CSRFToken: row.CSRFToken,
		ExpiresAt: row.ExpiresAt,
	}
}

type storeItemSettingRow struct {
	ItemType  string `gorm:"column:item_type;primaryKey"`
	Price     int    `gorm:"column:price"`
	CreatedAt int64  `gorm:"column:created_at;autoCreateTime:false"`
	UpdatedAt int64  `gorm:"column:updated_at;autoUpdateTime:false"`
	UpdatedBy *int64 `gorm:"column:updated_by"`
}

func (storeItemSettingRow) TableName() string { return "store_item_settings" }

func (row storeItemSettingRow) entity() entity.StoreItemSetting {
	return entity.StoreItemSetting{
		ItemType:  row.ItemType,
		Price:     row.Price,
		CreatedAt: row.CreatedAt,
		UpdatedAt: row.UpdatedAt,
		UpdatedBy: row.UpdatedBy,
	}
}

type ticketRow struct {
	ID          int     `gorm:"column:id;primaryKey"`
	Title       string  `gorm:"column:title"`
	Description string  `gorm:"column:description"`
	GuildID     int64   `gorm:"column:guild_id"`
	ChannelID   int64   `gorm:"column:channel_id"`
	InitiatorID int64   `gorm:"column:initiator_id"`
	CloseReason *string `gorm:"column:close_reason"`
	ClosedByID  *int64  `gorm:"column:closed_by_id"`
	MergedInto  *int    `gorm:"column:merged_into"`
	CreatedAt   int64   `gorm:"column:created_at;autoCreateTime:false"`
	UpdatedAt   int64   `gorm:"column:updated_at;autoUpdateTime:false"`
}

func (ticketRow) TableName() string { return "support_tickets" }

func (row ticketRow) entity() entity.Ticket {
	return entity.Ticket{
		ID:          row.ID,
		Title:       row.Title,
		Description: row.Description,
		GuildID:     row.GuildID,
		ChannelID:   row.ChannelID,
		InitiatorID: row.InitiatorID,
		CloseReason: row.CloseReason,
		ClosedByID:  row.ClosedByID,
		MergedInto:  row.MergedInto,
		CreatedAt:   row.CreatedAt,
		UpdatedAt:   row.UpdatedAt,
	}
}

type userRow struct {
	ID         int64   `gorm:"column:id;primaryKey;autoIncrement:false"`
	Username   *string `gorm:"column:name"`
	GlobalName *string `gorm:"column:global_name"`
	AvatarHash *string `gorm:"column:avatar_hash"`
	IsBot      bool    `gorm:"column:is_bot"`
}

func (userRow) TableName() string { return "users" }

func (row userRow) entity() entity.User {
	return entity.User{
		ID:         row.ID,
		Username:   row.Username,
		GlobalName: row.GlobalName,
		AvatarHash: row.AvatarHash,
		IsBot:      row.IsBot,
	}
}
