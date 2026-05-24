package service

import (
	"fmt"
	"reflect"
	"strings"
)

const onlineStatusReference = "https://discord.com/developers/docs/events/gateway-events#presence"

func checkLevelRoles(lrd *LevelsRolesData) *APIError {
	arrLength := len(lrd.LevelsRoles)
	if arrLength == 0 || arrLength > MaxLevelRoleLength {
		return ErrorInvalidArrayLength(arrLength, MaxLevelRoleLength)
	}

	if hasNull(lrd.LevelsRoles) {
		return ErrorArrayHasNulls
	}
	if !IsColorValid(lrd.BackgroundColor) {
		return ErrorInvalidColor("background_color", lrd.BackgroundColor)
	}
	if err := checkGuild(lrd.Guild); err != nil {
		return err
	}
	if checkDuplicateLevel(lrd.LevelsRoles) {
		return ErrorDuplicateLevelRole
	}

	for _, lr := range lrd.LevelsRoles {
		if err := checkLevelRole(lr); err != nil {
			return err
		}
	}
	return nil
}

func checkGuild(guild *GuildDTO) *APIError {
	if guild == nil {
		return ErrorMissingFields("guild")
	}
	if strings.TrimSpace(guild.Name) == "" {
		return ErrorMissingFields("guild.name")
	}
	if strings.TrimSpace(guild.IconUrl) == "" {
		return ErrorMissingFields("guild.icon_url")
	}
	return nil
}

func checkDuplicateLevel(rls []*LevelRoleDTO) bool {
	seen := make(map[int]bool)

	for _, rl := range rls {
		if seen[rl.Level] {
			return true
		}
		seen[rl.Level] = true
	}
	return false
}

func checkLevelRole(lr *LevelRoleDTO) *APIError {
	if strings.TrimSpace(lr.Name) == "" {
		return ErrorMissingFields("name")
	}

	if lr.Level < 0 {
		return ErrorInvalidValue("level", lr.Level, "0+")
	}

	if !IsColorValid(lr.Color) {
		return ErrorInvalidColor("color", lr.Color)
	}
	return nil
}

func checkLevelData(ld *LevelDataDTO) *APIError {
	if strings.TrimSpace(ld.Username) == "" {
		return ErrorMissingFields("username")
	}

	if !IsColorValid(ld.ThemeColor) {
		return ErrorInvalidValue("theme_color", ld.ThemeColor, fmt.Sprintf("0 - %d", MaxColorValue))
	}

	// Validating numbers
	if err := checkPositive(ld.Rank, "rank"); err != nil {
		return err
	}
	if err := checkPositive(ld.Level, "level"); err != nil {
		return err
	}
	if err := checkPositive(ld.Xp, "xp"); err != nil {
		return err
	}
	if err := checkPositive(ld.XpNext, "xp_next"); err != nil || ld.XpNext == 0 {
		return ErrorCannotBeZero("xp_next", ld.XpNext)
	}
	if ld.Xp > ld.XpNext {
		return ErrorXpGreaterThanNext
	}

	_, ok := OnlineColors[ld.OnlineStatus]
	if !ok {
		return ErrorInvalidValue("online_status", ld.OnlineStatus, onlineStatusReference)
	}
	return nil
}

// hasNull checks if there is, at least one nil element in the given array.
// It panics if the argument is not an array.
func hasNull(arr any) bool {
	v := reflect.ValueOf(arr)
	if v.Kind() != reflect.Slice {
		panic("hasNull: expected a slice")
	}

	for i := 0; i < v.Len(); i++ {
		if v.Index(i).IsZero() {
			return true
		}
	}
	return false
}

func checkPositive(value int, fieldName string) *APIError {
	if value < 0 {
		return ErrorValueMustBePositive(fieldName, value)
	}
	return nil
}
