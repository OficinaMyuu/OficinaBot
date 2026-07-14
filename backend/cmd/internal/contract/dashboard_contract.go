package contract

type BirthdayRequest struct {
	UserID    string `json:"user_id"`
	Name      string `json:"name"`
	Birthday  string `json:"birthday"`
	ZoneHours int    `json:"zone_hours"`
}

type BirthdayResponse struct {
	UserID    string `json:"user_id"`
	Name      string `json:"name"`
	Birthday  string `json:"birthday"`
	ZoneHours int    `json:"zone_hours"`
	CreatedAt string `json:"created_at"`
	UpdatedAt string `json:"updated_at"`
}

type BirthdayListResponse struct {
	Birthdays []BirthdayResponse `json:"birthdays"`
}

type TicketListResponse struct {
	Tickets    []TicketResponse `json:"tickets"`
	NextCursor *string          `json:"next_cursor"`
}

type ChannelMessagesResponse struct {
	ChannelID     string            `json:"channel_id"`
	Messages      []MessageResponse `json:"messages"`
	HasMoreBefore bool              `json:"has_more_before"`
	HasMoreAfter  bool              `json:"has_more_after"`
}

type MessageVersionsResponse struct {
	Versions []MessageVersionResponse `json:"versions"`
}

type TicketResponse struct {
	ID          int     `json:"id"`
	Title       string  `json:"title"`
	Description string  `json:"description"`
	GuildID     string  `json:"guild_id"`
	ChannelID   string  `json:"channel_id"`
	InitiatorID string  `json:"initiator_id"`
	Status      string  `json:"status"`
	CloseReason *string `json:"close_reason"`
	ClosedByID  *string `json:"closed_by_id"`
	MergedInto  *int    `json:"merged_into"`
	CreatedAt   string  `json:"created_at"`
	UpdatedAt   string  `json:"updated_at"`
}

type MessageResponse struct {
	MessageID          string  `json:"message_id"`
	AuthorID           string  `json:"author_id"`
	MessageReferenceID *string `json:"message_reference_id"`
	Content            *string `json:"content"`
	StickerID          *string `json:"sticker_id"`
	IsEdited           bool    `json:"is_edited"`
	RevisionCount      int     `json:"revision_count"`
	IsDeleted          bool    `json:"is_deleted"`
	DeletedByID        *string `json:"deleted_by_id"`
	CreatedAt          string  `json:"created_at"`
	UpdatedAt          string  `json:"updated_at"`
}

type MessageVersionResponse struct {
	MessageID          string  `json:"message_id"`
	AuthorID           string  `json:"author_id"`
	MessageReferenceID *string `json:"message_reference_id"`
	Content            *string `json:"content"`
	StickerID          *string `json:"sticker_id"`
	CreatedAt          string  `json:"created_at"`
}

type StoreItemSettingsUpdateRequest struct {
	Price int `json:"price"`
}

type StoreItemSettingsResponse struct {
	ItemType  string  `json:"item_type"`
	Price     int     `json:"price"`
	CreatedAt string  `json:"created_at"`
	UpdatedAt string  `json:"updated_at"`
	UpdatedBy *string `json:"updated_by"`
}

type StoreItemSettingsListResponse struct {
	Items []StoreItemSettingsResponse `json:"items"`
}

type UserQueryRequest struct {
	UserIDs []string `json:"user_ids"`
}

type UserQueryResponse struct {
	Users []UserResponse `json:"users"`
}

type GuildDirectoryResponse struct {
	Channels []GuildChannelResponse `json:"channels"`
	Roles    []GuildRoleResponse    `json:"roles"`
}

type GuildChannelResponse struct {
	ID   string `json:"id"`
	Name string `json:"name"`
}

type GuildRoleResponse struct {
	ID   string `json:"id"`
	Name string `json:"name"`
}

type UserResponse struct {
	ID          string  `json:"id"`
	Username    *string `json:"username"`
	GlobalName  *string `json:"global_name"`
	DisplayName string  `json:"display_name"`
	AvatarHash  *string `json:"avatar_hash"`
	AvatarURL   string  `json:"avatar_url"`
	IsBot       bool    `json:"is_bot"`
}

type DashboardSessionResponse struct {
	User      DashboardUserResponse `json:"user"`
	CSRFToken string                `json:"csrf_token"`
}

type DashboardUserResponse struct {
	ID           string  `json:"id"`
	Username     string  `json:"username"`
	GlobalName   *string `json:"global_name"`
	AvatarURL    *string `json:"avatar_url"`
	GuildName    string  `json:"guild_name"`
	GuildIconURL *string `json:"guild_icon_url"`
	Permissions  string  `json:"permissions"`
}

type ErrorResponse struct {
	Status  int    `json:"status"`
	Message string `json:"message"`
}
