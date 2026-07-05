package bot

import (
	"errors"
	"fmt"
	"log/slog"
	"slices"
	"sync"
	"time"

	"github.com/bwmarrin/discordgo"

	"oficina-registrar/internal/registration"
)

type DiscordService struct {
	roleCache           *RoleCache
	temporaryMessageTTL time.Duration
	logger              *slog.Logger
}

func NewDiscordService(roleCacheTTL time.Duration, temporaryMessageTTL time.Duration, logger *slog.Logger) *DiscordService {
	return &DiscordService{
		roleCache:           NewRoleCache(roleCacheTTL),
		temporaryMessageTTL: temporaryMessageTTL,
		logger:              logger,
	}
}

func (d *DiscordService) DeleteMessage(session *discordgo.Session, channelID string, messageID string) {
	if err := session.ChannelMessageDelete(channelID, messageID); err != nil && !IsUnknownMessage(err) {
		d.logger.Warn("could not delete Discord message", "channel_id", channelID, "message_id", messageID, "error", err)
	}
}

func (d *DiscordService) SendTemporaryMessage(session *discordgo.Session, channelID string, content string) {
	message, err := session.ChannelMessageSend(channelID, content)
	if err != nil {
		d.logger.Warn("could not send temporary Discord message", "channel_id", channelID, "error", err)
		return
	}

	ttl := d.temporaryMessageTTL
	if ttl <= 0 {
		return
	}

	time.AfterFunc(ttl, func() {
		d.DeleteMessage(session, channelID, message.ID)
	})
}

func (d *DiscordService) FetchMember(session *discordgo.Session, guildID string, userID string) (*discordgo.Member, error) {
	member, err := session.GuildMember(guildID, userID)
	if err != nil {
		return nil, fmt.Errorf("fetch guild member %s: %w", userID, err)
	}
	return member, nil
}

func (d *DiscordService) MemberHasRole(member *discordgo.Member, role registration.Role) bool {
	if member == nil {
		return false
	}
	return slices.Contains(member.Roles, role.ID())
}

func (d *DiscordService) HasGuildPermission(session *discordgo.Session, guildID string, member *discordgo.Member, permission int64) (bool, error) {
	if member == nil {
		return false, nil
	}

	roles, err := d.roleCache.Roles(session, guildID)
	if err != nil {
		return false, err
	}

	rolePermissions := make(map[string]int64, len(roles))
	for _, role := range roles {
		rolePermissions[role.ID] = role.Permissions
	}

	var permissions int64
	permissions |= rolePermissions[guildID]
	for _, roleID := range member.Roles {
		permissions |= rolePermissions[roleID]
	}

	return permissions&discordgo.PermissionAdministrator != 0 || permissions&permission != 0, nil
}

func (d *DiscordService) HasRoleOrPermission(session *discordgo.Session, guildID string, member *discordgo.Member, role registration.Role, permission int64) (bool, error) {
	if d.MemberHasRole(member, role) {
		return true, nil
	}
	return d.HasGuildPermission(session, guildID, member, permission)
}

func (d *DiscordService) EnsureRolesExist(session *discordgo.Session, guildID string, roles []registration.Role) (registration.Role, error) {
	guildRoles, err := d.roleCache.Roles(session, guildID)
	if err != nil {
		return "", err
	}

	exists := make(map[string]struct{}, len(guildRoles))
	for _, role := range guildRoles {
		exists[role.ID] = struct{}{}
	}

	for _, role := range roles {
		if _, ok := exists[role.ID()]; !ok {
			return role, nil
		}
	}
	return "", nil
}

func (d *DiscordService) ModifyMemberRoles(session *discordgo.Session, guildID string, member *discordgo.Member, add []registration.Role, remove []registration.Role) error {
	if member == nil || member.User == nil {
		return errors.New("member is nil")
	}

	roles := MergeMemberRoles(member.Roles, add, remove)
	_, err := session.GuildMemberEdit(guildID, member.User.ID, &discordgo.GuildMemberParams{Roles: &roles})
	if err != nil {
		return fmt.Errorf("modify member roles: %w", err)
	}
	return nil
}

func (d *DiscordService) DeleteRecentMessagesByAuthor(session *discordgo.Session, channelID string, authorID string, limit int) {
	messages, err := session.ChannelMessages(channelID, limit, "", "", "")
	if err != nil {
		d.logger.Warn("could not retrieve channel history", "channel_id", channelID, "error", err)
		return
	}

	for _, message := range messages {
		if message.Author != nil && message.Author.ID == authorID {
			d.DeleteMessage(session, channelID, message.ID)
		}
	}
}

func MergeMemberRoles(current []string, add []registration.Role, remove []registration.Role) []string {
	removed := make(map[string]struct{}, len(remove))
	for _, role := range remove {
		removed[role.ID()] = struct{}{}
	}

	seen := make(map[string]struct{}, len(current)+len(add))
	merged := make([]string, 0, len(current)+len(add))
	for _, roleID := range current {
		if _, ok := removed[roleID]; ok {
			continue
		}
		if _, ok := seen[roleID]; ok {
			continue
		}
		seen[roleID] = struct{}{}
		merged = append(merged, roleID)
	}

	for _, role := range add {
		roleID := role.ID()
		if _, ok := seen[roleID]; ok {
			continue
		}
		seen[roleID] = struct{}{}
		merged = append(merged, roleID)
	}

	return merged
}

func IsUnknownMessage(err error) bool {
	var restErr *discordgo.RESTError
	if !errors.As(err, &restErr) || restErr.Message == nil {
		return false
	}
	return restErr.Message.Code == discordgo.ErrCodeUnknownMessage
}

type RoleCache struct {
	ttl     time.Duration
	mu      sync.Mutex
	entries map[string]roleCacheEntry
}

type roleCacheEntry struct {
	expiresAt time.Time
	roles     []*discordgo.Role
}

func NewRoleCache(ttl time.Duration) *RoleCache {
	return &RoleCache{
		ttl:     ttl,
		entries: make(map[string]roleCacheEntry),
	}
}

func (c *RoleCache) Roles(session *discordgo.Session, guildID string) ([]*discordgo.Role, error) {
	now := time.Now()

	c.mu.Lock()
	entry, ok := c.entries[guildID]
	if ok && c.ttl > 0 && now.Before(entry.expiresAt) {
		roles := slices.Clone(entry.roles)
		c.mu.Unlock()
		return roles, nil
	}
	c.mu.Unlock()

	roles, err := session.GuildRoles(guildID)
	if err != nil {
		return nil, fmt.Errorf("fetch guild roles: %w", err)
	}

	c.mu.Lock()
	c.entries[guildID] = roleCacheEntry{
		expiresAt: now.Add(c.ttl),
		roles:     slices.Clone(roles),
	}
	c.mu.Unlock()

	return roles, nil
}
