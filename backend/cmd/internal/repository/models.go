package repository

import "time"

type User struct {
	DiscordID string    `gorm:"column:discord_id;primaryKey"`
	Username  string    `gorm:"column:username"`
	CreatedAt time.Time `gorm:"column:created_at"`
}

func (User) TableName() string {
	return "users"
}

type BotClient struct {
	Name       string     `gorm:"column:name;primaryKey"`
	TokenHash  string     `gorm:"column:token_hash"`
	CreatedAt  time.Time  `gorm:"column:created_at"`
	LastSeenAt *time.Time `gorm:"column:last_seen_at"`
}

func (BotClient) TableName() string {
	return "bot_clients"
}

type EventBatch struct {
	ID         string    `gorm:"column:id;primaryKey"`
	ClientName string    `gorm:"column:client_name"`
	Kind       string    `gorm:"column:kind"`
	ReceivedAt time.Time `gorm:"column:received_at"`
}

func (EventBatch) TableName() string {
	return "event_batches"
}

type MessageLog struct {
	ID        int64     `gorm:"column:id;primaryKey;autoIncrement"`
	BatchID   string    `gorm:"column:batch_id"`
	GuildID   string    `gorm:"column:guild_id"`
	ChannelID string    `gorm:"column:channel_id"`
	MessageID string    `gorm:"column:message_id"`
	AuthorID  string    `gorm:"column:author_id"`
	Content   string    `gorm:"column:content"`
	CreatedAt time.Time `gorm:"column:created_at"`
}

func (MessageLog) TableName() string {
	return "message_logs"
}

type Punishment struct {
	ID          int64     `gorm:"column:id;primaryKey;autoIncrement"`
	GuildID     string    `gorm:"column:guild_id"`
	UserID      string    `gorm:"column:user_id"`
	ModeratorID *string   `gorm:"column:moderator_id"`
	Type        string    `gorm:"column:type"`
	Reason      *string   `gorm:"column:reason"`
	SourceID    *string   `gorm:"column:source_id"`
	CreatedAt   time.Time `gorm:"column:created_at"`
}

func (Punishment) TableName() string {
	return "punishments"
}

type ConfigVersion struct {
	ID                 int64     `gorm:"column:id;primaryKey;autoIncrement"`
	Scope              string    `gorm:"column:scope"`
	Key                string    `gorm:"column:key"`
	ValueJSON          string    `gorm:"column:value_json"`
	CreatedByDiscordID *string   `gorm:"column:created_by_discord_id"`
	CreatedAt          time.Time `gorm:"column:created_at"`
}

func (ConfigVersion) TableName() string {
	return "config_versions"
}

type ConfigAcknowledgement struct {
	ID              int64     `gorm:"column:id;primaryKey;autoIncrement"`
	ConfigVersionID int64     `gorm:"column:config_version_id"`
	BotClientName   string    `gorm:"column:bot_client_name"`
	AckedAt         time.Time `gorm:"column:acked_at"`
}

func (ConfigAcknowledgement) TableName() string {
	return "config_acknowledgements"
}

type AuditAction struct {
	ID             int64     `gorm:"column:id;primaryKey;autoIncrement"`
	ActorDiscordID *string   `gorm:"column:actor_discord_id"`
	Action         string    `gorm:"column:action"`
	TargetType     string    `gorm:"column:target_type"`
	TargetID       *string   `gorm:"column:target_id"`
	MetadataJSON   string    `gorm:"column:metadata_json"`
	CreatedAt      time.Time `gorm:"column:created_at"`
}

func (AuditAction) TableName() string {
	return "audit_actions"
}

type AdminSession struct {
	TokenHash  string    `gorm:"column:token_hash;primaryKey"`
	DiscordID  string    `gorm:"column:discord_id"`
	CreatedAt  time.Time `gorm:"column:created_at"`
	LastSeenAt time.Time `gorm:"column:last_seen_at"`
	ExpiresAt  time.Time `gorm:"column:expires_at"`
	User       User      `gorm:"foreignKey:DiscordID;references:DiscordID"`
}

func (AdminSession) TableName() string {
	return "admin_sessions"
}
