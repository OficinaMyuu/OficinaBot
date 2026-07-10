package handler

import (
	"context"
	"errors"
	"net/http"
	"strings"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/contract"
	"oficina-img/internal/domain/entity"
	"oficina-img/internal/domain/mysql/repository"
	"oficina-img/internal/utils"
)

type StoreItemSettingsRepository interface {
	List(ctx context.Context) ([]entity.StoreItemSetting, error)
	Update(ctx context.Context, itemType string, price int, updatedBy int64) (entity.StoreItemSetting, error)
}

type StoreItemSettingsHandler struct {
	repository StoreItemSettingsRepository
}

func NewStoreItemSettingsHandler(repository StoreItemSettingsRepository) *StoreItemSettingsHandler {
	return &StoreItemSettingsHandler{repository: repository}
}

func (h *StoreItemSettingsHandler) List(c echo.Context) error {
	items, err := h.repository.List(c.Request().Context())
	if err != nil {
		return jsonError(c, http.StatusInternalServerError, "Could not list action costs")
	}

	response := make([]contract.StoreItemSettingsResponse, 0, len(items))
	for _, item := range items {
		response = append(response, toStoreItemSettingsResponse(item))
	}
	return c.JSON(http.StatusOK, contract.StoreItemSettingsListResponse{Items: response})
}

func (h *StoreItemSettingsHandler) Update(c echo.Context) error {
	itemType := strings.TrimSpace(c.Param("itemType"))
	if itemType == "" {
		return jsonError(c, http.StatusBadRequest, "Action type is required")
	}

	var req contract.StoreItemSettingsUpdateRequest
	if err := c.Bind(&req); err != nil {
		return jsonError(c, http.StatusBadRequest, "Malformed JSON body")
	}
	if req.Price < 0 {
		return jsonError(c, http.StatusBadRequest, "Price must be non-negative")
	}
	session, ok := dashboardSession(c)
	if !ok {
		return jsonError(c, http.StatusUnauthorized, "Not authenticated")
	}
	updatedBy, err := parseUserID(session.User.ID)
	if err != nil {
		return jsonError(c, http.StatusInternalServerError, "Dashboard session user id is invalid")
	}

	item, err := h.repository.Update(c.Request().Context(), itemType, req.Price, updatedBy)
	if errors.Is(err, repository.ErrStoreItemSettingNotFound) {
		return jsonError(c, http.StatusNotFound, "Action cost not found")
	}
	if err != nil {
		return jsonError(c, http.StatusInternalServerError, "Could not update action cost")
	}

	return c.JSON(http.StatusOK, toStoreItemSettingsResponse(item))
}

func toStoreItemSettingsResponse(item entity.StoreItemSetting) contract.StoreItemSettingsResponse {
	return contract.StoreItemSettingsResponse{
		ItemType:  item.ItemType,
		Price:     item.Price,
		CreatedAt: utils.FormatEpoch(item.CreatedAt),
		UpdatedAt: utils.FormatEpoch(item.UpdatedAt),
		UpdatedBy: optionalInt64String(item.UpdatedBy),
	}
}
