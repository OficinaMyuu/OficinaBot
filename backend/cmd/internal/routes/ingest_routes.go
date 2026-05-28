package routes

import (
	"context"
	"encoding/json"
	"net/http"
	"strings"
	"time"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/repository"
)

type EventBatchStore interface {
	Create(ctx context.Context, batch *repository.EventBatch) error
	Exists(ctx context.Context, id string) (bool, error)
}

type MessageLogStore interface {
	CreateMany(ctx context.Context, logs []repository.MessageLog) error
}

type PunishmentStore interface {
	Create(ctx context.Context, punishment *repository.Punishment) error
}

type RegistrationStore interface {
	CreateMany(ctx context.Context, registrations []repository.Registration) error
}

type SyncHeartbeatStore interface {
	Create(ctx context.Context, heartbeat *repository.SyncHeartbeat) error
}

type IngestHandler struct {
	batches       EventBatchStore
	messageLogs   MessageLogStore
	punishments   PunishmentStore
	registrations RegistrationStore
	heartbeats    SyncHeartbeatStore
}

func NewIngestHandler(
	batches EventBatchStore,
	messageLogs MessageLogStore,
	punishments PunishmentStore,
	registrations RegistrationStore,
	heartbeats SyncHeartbeatStore,
) *IngestHandler {
	return &IngestHandler{
		batches:       batches,
		messageLogs:   messageLogs,
		punishments:   punishments,
		registrations: registrations,
		heartbeats:    heartbeats,
	}
}

func (h *IngestHandler) MessageLogs(c echo.Context) error {
	client, ok := ServiceClient(c)
	if !ok {
		return c.JSON(http.StatusUnauthorized, authErrorResponse("Missing service client"))
	}

	var req messageLogsBatchRequest
	if err := c.Bind(&req); err != nil {
		return c.JSON(http.StatusBadRequest, authErrorResponse("Malformed JSON body"))
	}
	if strings.TrimSpace(req.BatchID) == "" || len(req.Logs) == 0 {
		return c.JSON(http.StatusBadRequest, authErrorResponse("batch_id and logs are required"))
	}

	created, err := h.createBatch(c.Request().Context(), req.BatchID, client.Name, "message_logs")
	if err != nil {
		return err
	}
	if !created {
		return c.JSON(http.StatusOK, ingestResponse{Status: "duplicate"})
	}

	logs := make([]repository.MessageLog, 0, len(req.Logs))
	for _, log := range req.Logs {
		logs = append(logs, repository.MessageLog{
			BatchID:   req.BatchID,
			GuildID:   log.GuildID,
			ChannelID: log.ChannelID,
			MessageID: log.MessageID,
			AuthorID:  log.AuthorID,
			Content:   log.Content,
			CreatedAt: log.CreatedAt,
		})
	}
	if err := h.messageLogs.CreateMany(c.Request().Context(), logs); err != nil {
		return err
	}
	return c.JSON(http.StatusCreated, ingestResponse{Status: "created"})
}

func (h *IngestHandler) Punishments(c echo.Context) error {
	client, ok := ServiceClient(c)
	if !ok {
		return c.JSON(http.StatusUnauthorized, authErrorResponse("Missing service client"))
	}

	var req punishmentsBatchRequest
	if err := c.Bind(&req); err != nil {
		return c.JSON(http.StatusBadRequest, authErrorResponse("Malformed JSON body"))
	}
	if strings.TrimSpace(req.BatchID) == "" || len(req.Punishments) == 0 {
		return c.JSON(http.StatusBadRequest, authErrorResponse("batch_id and punishments are required"))
	}

	created, err := h.createBatch(c.Request().Context(), req.BatchID, client.Name, "punishments")
	if err != nil {
		return err
	}
	if !created {
		return c.JSON(http.StatusOK, ingestResponse{Status: "duplicate"})
	}

	for _, punishment := range req.Punishments {
		if err := h.punishments.Create(c.Request().Context(), &repository.Punishment{
			GuildID:     punishment.GuildID,
			UserID:      punishment.UserID,
			ModeratorID: punishment.ModeratorID,
			Type:        punishment.Type,
			Reason:      punishment.Reason,
			SourceID:    punishment.SourceID,
			CreatedAt:   punishment.CreatedAt,
		}); err != nil {
			return err
		}
	}
	return c.JSON(http.StatusCreated, ingestResponse{Status: "created"})
}

func (h *IngestHandler) Registrations(c echo.Context) error {
	client, ok := ServiceClient(c)
	if !ok {
		return c.JSON(http.StatusUnauthorized, authErrorResponse("Missing service client"))
	}

	var req registrationsBatchRequest
	if err := c.Bind(&req); err != nil {
		return c.JSON(http.StatusBadRequest, authErrorResponse("Malformed JSON body"))
	}
	if strings.TrimSpace(req.BatchID) == "" || len(req.Registrations) == 0 {
		return c.JSON(http.StatusBadRequest, authErrorResponse("batch_id and registrations are required"))
	}

	created, err := h.createBatch(c.Request().Context(), req.BatchID, client.Name, "registrations")
	if err != nil {
		return err
	}
	if !created {
		return c.JSON(http.StatusOK, ingestResponse{Status: "duplicate"})
	}

	registrations := make([]repository.Registration, 0, len(req.Registrations))
	for _, registration := range req.Registrations {
		registrations = append(registrations, repository.Registration{
			BatchID:      req.BatchID,
			GuildID:      registration.GuildID,
			UserID:       registration.UserID,
			Username:     registration.Username,
			RegisteredAt: registration.RegisteredAt,
			MetadataJSON: rawJSONOrEmpty(registration.Metadata),
		})
	}
	if err := h.registrations.CreateMany(c.Request().Context(), registrations); err != nil {
		return err
	}
	return c.JSON(http.StatusCreated, ingestResponse{Status: "created"})
}

func (h *IngestHandler) SyncHeartbeat(c echo.Context) error {
	client, ok := ServiceClient(c)
	if !ok {
		return c.JSON(http.StatusUnauthorized, authErrorResponse("Missing service client"))
	}

	var req syncHeartbeatRequest
	if err := c.Bind(&req); err != nil {
		return c.JSON(http.StatusBadRequest, authErrorResponse("Malformed JSON body"))
	}
	if strings.TrimSpace(req.Status) == "" {
		return c.JSON(http.StatusBadRequest, authErrorResponse("status is required"))
	}

	if err := h.heartbeats.Create(c.Request().Context(), &repository.SyncHeartbeat{
		ClientName:  client.Name,
		Status:      req.Status,
		DetailsJSON: rawJSONOrEmpty(req.Details),
		CheckedAt:   req.CheckedAt,
	}); err != nil {
		return err
	}
	return c.JSON(http.StatusCreated, ingestResponse{Status: "created"})
}

func (h *IngestHandler) createBatch(ctx context.Context, batchID, clientName, kind string) (bool, error) {
	exists, err := h.batches.Exists(ctx, batchID)
	if err != nil {
		return false, err
	}
	if exists {
		return false, nil
	}
	return true, h.batches.Create(ctx, &repository.EventBatch{ID: batchID, ClientName: clientName, Kind: kind})
}

func rawJSONOrEmpty(raw json.RawMessage) string {
	if len(raw) == 0 {
		return "{}"
	}
	return string(raw)
}

type ingestResponse struct {
	Status string `json:"status"`
}

type messageLogsBatchRequest struct {
	BatchID string              `json:"batch_id"`
	Logs    []messageLogRequest `json:"logs"`
}

type messageLogRequest struct {
	GuildID   string    `json:"guild_id"`
	ChannelID string    `json:"channel_id"`
	MessageID string    `json:"message_id"`
	AuthorID  string    `json:"author_id"`
	Content   string    `json:"content"`
	CreatedAt time.Time `json:"created_at"`
}

type punishmentsBatchRequest struct {
	BatchID     string              `json:"batch_id"`
	Punishments []punishmentRequest `json:"punishments"`
}

type punishmentRequest struct {
	GuildID     string    `json:"guild_id"`
	UserID      string    `json:"user_id"`
	ModeratorID *string   `json:"moderator_id"`
	Type        string    `json:"type"`
	Reason      *string   `json:"reason"`
	SourceID    *string   `json:"source_id"`
	CreatedAt   time.Time `json:"created_at"`
}

type registrationsBatchRequest struct {
	BatchID       string                `json:"batch_id"`
	Registrations []registrationRequest `json:"registrations"`
}

type registrationRequest struct {
	GuildID      string          `json:"guild_id"`
	UserID       string          `json:"user_id"`
	Username     string          `json:"username"`
	RegisteredAt time.Time       `json:"registered_at"`
	Metadata     json.RawMessage `json:"metadata"`
}

type syncHeartbeatRequest struct {
	Status    string          `json:"status"`
	Details   json.RawMessage `json:"details"`
	CheckedAt time.Time       `json:"checked_at"`
}
