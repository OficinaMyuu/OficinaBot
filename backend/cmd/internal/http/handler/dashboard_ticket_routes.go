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

func parseTicketID(value string) (int, error) {
	id, err := strconv.Atoi(value)
	if err != nil || id <= 0 {
		return 0, errors.New("ticket id must be a positive integer")
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

func optionalInt64String(value *int64) *string {
	if value == nil {
		return nil
	}
	formatted := strconv.FormatInt(*value, 10)
	return &formatted
}
