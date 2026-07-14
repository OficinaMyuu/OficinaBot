package entity

import "time"

type Birthday struct {
	UserID    int64
	Name      string
	Birthday  time.Time
	ZoneHours int
	CreatedAt int64
	UpdatedAt int64
}

type User struct {
	ID         int64
	Username   *string
	GlobalName *string
	AvatarHash *string
	IsBot      bool
}

type Ticket struct {
	ID          int
	Title       string
	Description string
	GuildID     int64
	ChannelID   int64
	InitiatorID int64
	CloseReason *string
	ClosedByID  *int64
	MergedInto  *int
	CreatedAt   int64
	UpdatedAt   int64
}

func (t Ticket) Status() string {
	if t.ClosedByID == nil {
		return "open"
	}
	return "closed"
}

type Message struct {
	MessageID          int64
	AuthorID           int64
	MessageReferenceID *int64
	Content            *string
	StickerID          *int64
	IsEdited           bool
	RevisionCount      int
	IsDeleted          bool
	DeletedByID        *int64
	CreatedAt          int64
	UpdatedAt          int64
}

type MessageVersion struct {
	MessageID          int64
	AuthorID           int64
	MessageReferenceID *int64
	Content            *string
	StickerID          *int64
	CreatedAt          int64
}

type StoreItemSetting struct {
	ItemType  string
	Price     int
	CreatedAt int64
	UpdatedAt int64
	UpdatedBy *int64
}

type DashboardUser struct {
	ID           string
	Username     string
	GlobalName   *string
	AvatarURL    *string
	GuildName    string
	GuildIconURL *string
	Permissions  string
}

type DashboardSession struct {
	ID        string
	User      DashboardUser
	CSRFToken string
	ExpiresAt int64
}
