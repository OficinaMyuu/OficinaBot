package handler

import (
	"context"
	"errors"
	"net/http"
	"strconv"
	"strings"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/contract"
	"oficina-img/internal/domain/entity"
	"oficina-img/internal/domain/mysql/repository"
	"oficina-img/internal/utils"
)

type TicketRepository interface {
	List(ctx context.Context, filter repository.TicketListFilter) (repository.TicketPage, error)
	Find(ctx context.Context, ticketID int) (entity.Ticket, error)
	ListMessages(ctx context.Context, channelID int64, filter repository.TicketMessageFilter) (repository.TicketMessagePage, error)
	ListMessageVersions(ctx context.Context, channelID, messageID int64) ([]entity.TicketMessageVersion, error)
}

func (h *TicketHandler) MessageVersions(c echo.Context) error {
	ticketID, err := parseTicketID(c.Param("ticketID"))
	if err != nil {
		return jsonError(c, http.StatusBadRequest, err.Error())
	}
	messageID, err := parseMessageID(c.Param("messageID"))
	if err != nil {
		return jsonError(c, http.StatusBadRequest, err.Error())
	}
	ticket, err := h.repository.Find(c.Request().Context(), ticketID)
	if err != nil {
		if errors.Is(err, repository.ErrTicketNotFound) {
			return jsonError(c, http.StatusNotFound, "Ticket not found")
		}
		return jsonError(c, http.StatusInternalServerError, "Could not load ticket")
	}
	versions, err := h.repository.ListMessageVersions(c.Request().Context(), ticket.ChannelID, messageID)
	if err != nil {
		return jsonError(c, http.StatusInternalServerError, "Could not load message versions")
	}
	response := make([]contract.TicketMessageVersionResponse, 0, len(versions))
	for _, version := range versions {
		response = append(response, toTicketMessageVersionResponse(version))
	}
	return c.JSON(http.StatusOK, contract.TicketMessageVersionsResponse{Versions: response})
}

type TicketHandler struct {
	repository TicketRepository
}

func NewTicketHandler(repository TicketRepository) *TicketHandler {
	return &TicketHandler{repository: repository}
}

func (h *TicketHandler) List(c echo.Context) error {
	limit, err := parseOptionalLimit(c.QueryParam("limit"), 25, 100)
	if err != nil {
		return jsonError(c, http.StatusBadRequest, err.Error())
	}
	cursor, err := repository.ParseTicketCursor(c.QueryParam("cursor"))
	if err != nil {
		return jsonError(c, http.StatusBadRequest, err.Error())
	}
	status, err := parseTicketStatus(c.QueryParam("status"))
	if err != nil {
		return jsonError(c, http.StatusBadRequest, err.Error())
	}

	page, err := h.repository.List(c.Request().Context(), repository.TicketListFilter{
		Search: c.QueryParam("search"),
		Status: status,
		Limit:  limit,
		Cursor: cursor,
	})
	if err != nil {
		return jsonError(c, http.StatusInternalServerError, "Could not list tickets")
	}

	response := make([]contract.TicketResponse, 0, len(page.Tickets))
	for _, ticket := range page.Tickets {
		response = append(response, toTicketResponse(ticket))
	}
	return c.JSON(http.StatusOK, contract.TicketListResponse{
		Tickets:    response,
		NextCursor: repository.FormatTicketCursor(page.NextCursor),
	})
}

func (h *TicketHandler) Messages(c echo.Context) error {
	ticketID, err := parseTicketID(c.Param("ticketID"))
	if err != nil {
		return jsonError(c, http.StatusBadRequest, err.Error())
	}
	limit, err := parseOptionalLimit(c.QueryParam("limit"), 50, 100)
	if err != nil {
		return jsonError(c, http.StatusBadRequest, err.Error())
	}
	cursor, err := repository.ParseTicketCursor(c.QueryParam("cursor"))
	if err != nil {
		return jsonError(c, http.StatusBadRequest, err.Error())
	}

	ticket, err := h.repository.Find(c.Request().Context(), ticketID)
	if err != nil {
		if errors.Is(err, repository.ErrTicketNotFound) {
			return jsonError(c, http.StatusNotFound, "Ticket not found")
		}
		return jsonError(c, http.StatusInternalServerError, "Could not load ticket")
	}

	page, err := h.repository.ListMessages(c.Request().Context(), ticket.ChannelID, repository.TicketMessageFilter{
		Limit:  limit,
		Cursor: cursor,
	})
	if err != nil {
		return jsonError(c, http.StatusInternalServerError, "Could not list ticket messages")
	}

	messages := make([]contract.TicketMessageResponse, 0, len(page.Messages))
	for _, message := range page.Messages {
		messages = append(messages, toTicketMessageResponse(message))
	}
	return c.JSON(http.StatusOK, contract.TicketMessagesResponse{
		Ticket:     toTicketResponse(ticket),
		Messages:   messages,
		NextCursor: repository.FormatTicketCursor(page.NextCursor),
	})
}

func parseTicketID(value string) (int, error) {
	id, err := strconv.Atoi(value)
	if err != nil || id <= 0 {
		return 0, errors.New("ticket id must be a positive integer")
	}
	return id, nil
}

func parseMessageID(value string) (int64, error) {
	id, err := strconv.ParseInt(value, 10, 64)
	if err != nil || id <= 0 {
		return 0, errors.New("message id must be a positive integer")
	}
	return id, nil
}

func parseOptionalLimit(value string, fallback, max int) (int, error) {
	if value == "" {
		return fallback, nil
	}
	limit, err := strconv.Atoi(value)
	if err != nil || limit <= 0 {
		return 0, errors.New("limit must be a positive integer")
	}
	if limit > max {
		return max, nil
	}
	return limit, nil
}

func parseTicketStatus(value string) (string, error) {
	status := strings.TrimSpace(strings.ToLower(value))
	if status == "" {
		return "all", nil
	}
	switch status {
	case "all", "open", "closed":
		return status, nil
	default:
		return "", errors.New("status must be all, open, or closed")
	}
}

func toTicketResponse(ticket entity.Ticket) contract.TicketResponse {
	return contract.TicketResponse{
		ID:          ticket.ID,
		Title:       ticket.Title,
		Description: ticket.Description,
		GuildID:     strconv.FormatInt(ticket.GuildID, 10),
		ChannelID:   strconv.FormatInt(ticket.ChannelID, 10),
		InitiatorID: strconv.FormatInt(ticket.InitiatorID, 10),
		Status:      ticket.Status(),
		CloseReason: ticket.CloseReason,
		ClosedByID:  optionalInt64String(ticket.ClosedByID),
		MergedInto:  ticket.MergedInto,
		CreatedAt:   utils.FormatEpoch(ticket.CreatedAt),
		UpdatedAt:   utils.FormatEpoch(ticket.UpdatedAt),
	}
}

func toTicketMessageResponse(message entity.TicketMessage) contract.TicketMessageResponse {
	return contract.TicketMessageResponse{
		MessageID:          strconv.FormatInt(message.MessageID, 10),
		AuthorID:           strconv.FormatInt(message.AuthorID, 10),
		MessageReferenceID: optionalInt64String(message.MessageReferenceID),
		Content:            message.Content,
		StickerID:          optionalInt64String(message.StickerID),
		IsEdited:           message.IsEdited,
		RevisionCount:      message.RevisionCount,
		IsDeleted:          message.IsDeleted,
		DeletedByID:        optionalInt64String(message.DeletedByID),
		CreatedAt:          utils.FormatEpoch(message.CreatedAt),
		UpdatedAt:          utils.FormatEpoch(message.UpdatedAt),
	}
}

func toTicketMessageVersionResponse(version entity.TicketMessageVersion) contract.TicketMessageVersionResponse {
	return contract.TicketMessageVersionResponse{
		MessageID:          strconv.FormatInt(version.MessageID, 10),
		AuthorID:           strconv.FormatInt(version.AuthorID, 10),
		MessageReferenceID: optionalInt64String(version.MessageReferenceID),
		Content:            version.Content,
		StickerID:          optionalInt64String(version.StickerID),
		CreatedAt:          utils.FormatEpoch(version.CreatedAt),
	}
}

func optionalInt64String(value *int64) *string {
	if value == nil {
		return nil
	}
	formatted := strconv.FormatInt(*value, 10)
	return &formatted
}
