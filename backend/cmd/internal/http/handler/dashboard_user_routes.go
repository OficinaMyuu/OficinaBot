package handler

import (
	"context"
	"fmt"
	"net/http"
	"strconv"
	"strings"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/contract"
	"oficina-img/internal/domain/entity"
)

const maxUserQueryIDs = 200

type UserRepository interface {
	FindMany(ctx context.Context, userIDs []int64) ([]entity.User, error)
}

type UserHandler struct {
	repository UserRepository
}

func NewUserHandler(repository UserRepository) *UserHandler {
	return &UserHandler{repository: repository}
}

func (h *UserHandler) Query(c echo.Context) error {
	var req contract.UserQueryRequest
	if err := c.Bind(&req); err != nil {
		return jsonError(c, http.StatusBadRequest, "Malformed JSON body")
	}

	userIDs, err := parseUserIDs(req.UserIDs)
	if err != nil {
		return jsonError(c, http.StatusBadRequest, err.Error())
	}

	users, err := h.repository.FindMany(c.Request().Context(), userIDs)
	if err != nil {
		return jsonError(c, http.StatusInternalServerError, "Could not query users")
	}

	response := make([]contract.UserResponse, 0, len(users))
	for _, user := range users {
		response = append(response, toUserResponse(user))
	}
	return c.JSON(http.StatusOK, contract.UserQueryResponse{Users: response})
}

func parseUserIDs(values []string) ([]int64, error) {
	if len(values) == 0 {
		return []int64{}, nil
	}
	if len(values) > maxUserQueryIDs {
		return nil, fmt.Errorf("user_ids must contain at most %d ids", maxUserQueryIDs)
	}

	seen := make(map[int64]struct{}, len(values))
	ids := make([]int64, 0, len(values))
	for _, value := range values {
		id, err := parseUserID(strings.TrimSpace(value))
		if err != nil {
			return nil, err
		}
		if _, ok := seen[id]; ok {
			continue
		}
		seen[id] = struct{}{}
		ids = append(ids, id)
	}
	return ids, nil
}

func toUserResponse(user entity.User) contract.UserResponse {
	return contract.UserResponse{
		ID:          strconv.FormatInt(user.ID, 10),
		Username:    user.Username,
		GlobalName:  user.GlobalName,
		DisplayName: userDisplayName(user),
		AvatarHash:  user.AvatarHash,
		AvatarURL:   userAvatarURL(user),
		IsBot:       user.IsBot,
	}
}

func userDisplayName(user entity.User) string {
	if user.GlobalName != nil && strings.TrimSpace(*user.GlobalName) != "" {
		return *user.GlobalName
	}
	if user.Username != nil && strings.TrimSpace(*user.Username) != "" {
		return *user.Username
	}
	return strconv.FormatInt(user.ID, 10)
}

func userAvatarURL(user entity.User) string {
	if user.AvatarHash != nil && strings.TrimSpace(*user.AvatarHash) != "" {
		return fmt.Sprintf("https://cdn.discordapp.com/avatars/%d/%s.png", user.ID, *user.AvatarHash)
	}
	index := (uint64(user.ID) >> 22) % 6
	return fmt.Sprintf("https://cdn.discordapp.com/embed/avatars/%d.png", index)
}
