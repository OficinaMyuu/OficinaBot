package routes

import (
	"context"
	"errors"
	"fmt"
	"net/http"
	"strconv"
	"strings"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/store"
)

type TicketRepository interface {
	List(ctx context.Context, filter store.TicketListFilter) (store.TicketPage, error)
	Find(ctx context.Context, ticketID int) (store.Ticket, error)
	ListMessages(ctx context.Context, channelID int64, filter store.TicketMessageFilter) (store.TicketMessagePage, error)
}

type TicketHandler struct {
	repository TicketRepository
}

type ticketListResponse struct {
	Tickets    []ticketResponse `json:"tickets"`
	NextCursor *string          `json:"next_cursor"`
}

type ticketMessagesResponse struct {
	Ticket     ticketResponse          `json:"ticket"`
	Messages   []ticketMessageResponse `json:"messages"`
	NextCursor *string                 `json:"next_cursor"`
}

type ticketResponse struct {
	ID          int                 `json:"id"`
	Title       string              `json:"title"`
	Description string              `json:"description"`
	GuildID     string              `json:"guild_id"`
	ChannelID   string              `json:"channel_id"`
	Initiator   ticketUserResponse  `json:"initiator"`
	Status      string              `json:"status"`
	CloseReason *string             `json:"close_reason"`
	ClosedBy    *ticketUserResponse `json:"closed_by"`
	MergedInto  *int                `json:"merged_into"`
	CreatedAt   int64               `json:"created_at"`
	UpdatedAt   int64               `json:"updated_at"`
}

type ticketMessageResponse struct {
	MessageID          string              `json:"message_id"`
	Author             ticketUserResponse  `json:"author"`
	MessageReferenceID *string             `json:"message_reference_id"`
	Content            *string             `json:"content"`
	StickerID          *string             `json:"sticker_id"`
	IsEdited           bool                `json:"is_edited"`
	IsDeleted          bool                `json:"is_deleted"`
	DeletedBy          *ticketUserResponse `json:"deleted_by"`
	CreatedAt          int64               `json:"created_at"`
	UpdatedAt          int64               `json:"updated_at"`
}

type ticketUserResponse struct {
	ID          string  `json:"id"`
	Username    *string `json:"username"`
	GlobalName  *string `json:"global_name"`
	DisplayName string  `json:"display_name"`
	AvatarURL   string  `json:"avatar_url"`
}

func NewTicketHandler(repository TicketRepository) *TicketHandler {
	return &TicketHandler{repository: repository}
}

func (h *TicketHandler) List(c echo.Context) error {
	limit, err := parseOptionalLimit(c.QueryParam("limit"), 25, 100)
	if err != nil {
		return jsonError(c, http.StatusBadRequest, err.Error())
	}
	cursor, err := store.ParseTicketCursor(c.QueryParam("cursor"))
	if err != nil {
		return jsonError(c, http.StatusBadRequest, err.Error())
	}
	status, err := parseTicketStatus(c.QueryParam("status"))
	if err != nil {
		return jsonError(c, http.StatusBadRequest, err.Error())
	}

	page, err := h.repository.List(c.Request().Context(), store.TicketListFilter{
		Search: c.QueryParam("search"),
		Status: status,
		Limit:  limit,
		Cursor: cursor,
	})
	if err != nil {
		return jsonError(c, http.StatusInternalServerError, "Could not list tickets")
	}

	response := make([]ticketResponse, 0, len(page.Tickets))
	for _, ticket := range page.Tickets {
		response = append(response, toTicketResponse(ticket))
	}
	return c.JSON(http.StatusOK, ticketListResponse{
		Tickets:    response,
		NextCursor: store.FormatTicketCursor(page.NextCursor),
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
	cursor, err := store.ParseTicketCursor(c.QueryParam("cursor"))
	if err != nil {
		return jsonError(c, http.StatusBadRequest, err.Error())
	}

	ticket, err := h.repository.Find(c.Request().Context(), ticketID)
	if err != nil {
		if errors.Is(err, store.ErrTicketNotFound) {
			return jsonError(c, http.StatusNotFound, "Ticket not found")
		}
		return jsonError(c, http.StatusInternalServerError, "Could not load ticket")
	}

	page, err := h.repository.ListMessages(c.Request().Context(), ticket.ChannelID, store.TicketMessageFilter{
		Limit:  limit,
		Cursor: cursor,
	})
	if err != nil {
		return jsonError(c, http.StatusInternalServerError, "Could not list ticket messages")
	}

	messages := make([]ticketMessageResponse, 0, len(page.Messages))
	for _, message := range page.Messages {
		messages = append(messages, toTicketMessageResponse(message))
	}
	return c.JSON(http.StatusOK, ticketMessagesResponse{
		Ticket:     toTicketResponse(ticket),
		Messages:   messages,
		NextCursor: store.FormatTicketCursor(page.NextCursor),
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

func toTicketResponse(ticket store.Ticket) ticketResponse {
	return ticketResponse{
		ID:          ticket.ID,
		Title:       ticket.Title,
		Description: ticket.Description,
		GuildID:     strconv.FormatInt(ticket.GuildID, 10),
		ChannelID:   strconv.FormatInt(ticket.ChannelID, 10),
		Initiator:   toTicketUserResponse(ticket.Initiator),
		Status:      ticket.Status(),
		CloseReason: ticket.CloseReason,
		ClosedBy:    toOptionalTicketUserResponse(ticket.ClosedBy),
		MergedInto:  ticket.MergedInto,
		CreatedAt:   ticket.CreatedAt,
		UpdatedAt:   ticket.UpdatedAt,
	}
}

func toTicketMessageResponse(message store.TicketMessage) ticketMessageResponse {
	return ticketMessageResponse{
		MessageID:          strconv.FormatInt(message.MessageID, 10),
		Author:             toTicketUserResponse(message.Author),
		MessageReferenceID: optionalInt64String(message.MessageReferenceID),
		Content:            message.Content,
		StickerID:          optionalInt64String(message.StickerID),
		IsEdited:           message.IsEdited,
		IsDeleted:          message.IsDeleted,
		DeletedBy:          toOptionalTicketUserResponse(message.DeletedBy),
		CreatedAt:          message.CreatedAt,
		UpdatedAt:          message.UpdatedAt,
	}
}

func toOptionalTicketUserResponse(user *store.TicketUser) *ticketUserResponse {
	if user == nil {
		return nil
	}
	response := toTicketUserResponse(*user)
	return &response
}

func toTicketUserResponse(user store.TicketUser) ticketUserResponse {
	return ticketUserResponse{
		ID:          strconv.FormatInt(user.ID, 10),
		Username:    user.Username,
		GlobalName:  user.GlobalName,
		DisplayName: ticketDisplayName(user),
		AvatarURL:   defaultAvatarURL(user.ID),
	}
}

func ticketDisplayName(user store.TicketUser) string {
	if user.GlobalName != nil && strings.TrimSpace(*user.GlobalName) != "" {
		return *user.GlobalName
	}
	if user.Username != nil && strings.TrimSpace(*user.Username) != "" {
		return *user.Username
	}
	return strconv.FormatInt(user.ID, 10)
}

func optionalInt64String(value *int64) *string {
	if value == nil {
		return nil
	}
	formatted := strconv.FormatInt(*value, 10)
	return &formatted
}

func defaultAvatarURL(userID int64) string {
	index := (uint64(userID) >> 22) % 6
	return fmt.Sprintf("https://cdn.discordapp.com/embed/avatars/%d.png", index)
}
