package handler

import (
	"context"
	"errors"
	"net/http"
	"strconv"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/contract"
	"oficina-img/internal/domain/entity"
	"oficina-img/internal/domain/mysql/repository"
	"oficina-img/internal/utils"
)

type MessageRepository interface {
	List(ctx context.Context, channelID int64, filter repository.MessageFilter) (repository.MessagePage, error)
	ListVersions(ctx context.Context, channelID, messageID int64) ([]entity.MessageVersion, error)
}

type ChannelMessageHandler struct {
	repository MessageRepository
}

func NewChannelMessageHandler(repository MessageRepository) *ChannelMessageHandler {
	return &ChannelMessageHandler{repository: repository}
}

func (h *ChannelMessageHandler) List(c echo.Context) error {
	channelID, err := parseSnowflake(c.Param("channelID"), "channel")
	if err != nil {
		return jsonError(c, http.StatusBadRequest, err.Error())
	}
	limit, err := parseOptionalLimit(c.QueryParam("limit"), 50, 100)
	if err != nil {
		return jsonError(c, http.StatusBadRequest, err.Error())
	}
	before, after, around, err := parseMessageAnchors(c)
	if err != nil {
		return jsonError(c, http.StatusBadRequest, err.Error())
	}
	page, err := h.repository.List(c.Request().Context(), channelID, repository.MessageFilter{
		Limit: limit, BeforeID: before, AfterID: after, AroundID: around,
	})
	if errors.Is(err, repository.ErrMessageNotFound) {
		return jsonError(c, http.StatusNotFound, "Message not found in channel")
	}
	if err != nil {
		return jsonError(c, http.StatusInternalServerError, "Could not list channel messages")
	}
	messages := make([]contract.MessageResponse, 0, len(page.Messages))
	for _, message := range page.Messages {
		messages = append(messages, toMessageResponse(message))
	}
	return c.JSON(http.StatusOK, contract.ChannelMessagesResponse{
		ChannelID: strconv.FormatInt(channelID, 10), Messages: messages,
		HasMoreBefore: page.HasMoreBefore, HasMoreAfter: page.HasMoreAfter,
	})
}

func (h *ChannelMessageHandler) Versions(c echo.Context) error {
	channelID, err := parseSnowflake(c.Param("channelID"), "channel")
	if err != nil {
		return jsonError(c, http.StatusBadRequest, err.Error())
	}
	messageID, err := parseSnowflake(c.Param("messageID"), "message")
	if err != nil {
		return jsonError(c, http.StatusBadRequest, err.Error())
	}
	versions, err := h.repository.ListVersions(c.Request().Context(), channelID, messageID)
	if errors.Is(err, repository.ErrMessageNotFound) {
		return jsonError(c, http.StatusNotFound, "Message not found in channel")
	}
	if err != nil {
		return jsonError(c, http.StatusInternalServerError, "Could not load message versions")
	}
	response := make([]contract.MessageVersionResponse, 0, len(versions))
	for _, version := range versions {
		response = append(response, toMessageVersionResponse(version))
	}
	return c.JSON(http.StatusOK, contract.MessageVersionsResponse{Versions: response})
}

func parseMessageAnchors(c echo.Context) (*int64, *int64, *int64, error) {
	values := []struct {
		name  string
		value string
	}{
		{name: "before", value: c.QueryParam("before")},
		{name: "after", value: c.QueryParam("after")},
		{name: "around", value: c.QueryParam("around")},
	}
	anchors := make([]*int64, len(values))
	present := 0
	for index, value := range values {
		if value.value == "" {
			continue
		}
		parsed, err := parseSnowflake(value.value, value.name)
		if err != nil {
			return nil, nil, nil, err
		}
		anchors[index] = &parsed
		present++
	}
	if present > 1 {
		return nil, nil, nil, errors.New("before, after, and around are mutually exclusive")
	}
	return anchors[0], anchors[1], anchors[2], nil
}

func parseSnowflake(value, field string) (int64, error) {
	id, err := strconv.ParseInt(value, 10, 64)
	if err != nil || id <= 0 {
		return 0, errors.New(field + " id must be a positive integer")
	}
	return id, nil
}

func toMessageResponse(message entity.Message) contract.MessageResponse {
	return contract.MessageResponse{
		MessageID: strconv.FormatInt(message.MessageID, 10), AuthorID: strconv.FormatInt(message.AuthorID, 10),
		MessageReferenceID: optionalInt64String(message.MessageReferenceID), Content: message.Content,
		StickerID: optionalInt64String(message.StickerID), IsEdited: message.IsEdited,
		RevisionCount: message.RevisionCount, IsDeleted: message.IsDeleted,
		DeletedByID: optionalInt64String(message.DeletedByID), CreatedAt: utils.FormatEpoch(message.CreatedAt), UpdatedAt: utils.FormatEpoch(message.UpdatedAt),
	}
}

func toMessageVersionResponse(version entity.MessageVersion) contract.MessageVersionResponse {
	return contract.MessageVersionResponse{
		MessageID: strconv.FormatInt(version.MessageID, 10), AuthorID: strconv.FormatInt(version.AuthorID, 10),
		MessageReferenceID: optionalInt64String(version.MessageReferenceID), Content: version.Content,
		StickerID: optionalInt64String(version.StickerID), CreatedAt: utils.FormatEpoch(version.CreatedAt),
	}
}
