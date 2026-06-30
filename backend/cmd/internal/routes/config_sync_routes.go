package routes

import (
	"context"
	"errors"
	"net/http"
	"strconv"

	mysqldriver "github.com/go-sql-driver/mysql"
	"github.com/labstack/echo/v4"
	"gorm.io/gorm"
	"oficina-img/internal/repository"
)

type ConfigSyncStore interface {
	PendingForClient(ctx context.Context, clientName string) ([]repository.ConfigVersion, error)
	Acknowledge(ctx context.Context, versionID int64, clientName string) error
}

type ConfigSyncHandler struct {
	configs ConfigSyncStore
}

func NewConfigSyncHandler(configs ConfigSyncStore) *ConfigSyncHandler {
	return &ConfigSyncHandler{configs: configs}
}

func (h *ConfigSyncHandler) Pending(c echo.Context) error {
	client, ok := ServiceClient(c)
	if !ok {
		return c.JSON(http.StatusUnauthorized, authErrorResponse("Missing service client"))
	}

	configs, err := h.configs.PendingForClient(c.Request().Context(), client.Name)
	if err != nil {
		return err
	}
	return c.JSON(http.StatusOK, configs)
}

func (h *ConfigSyncHandler) Ack(c echo.Context) error {
	client, ok := ServiceClient(c)
	if !ok {
		return c.JSON(http.StatusUnauthorized, authErrorResponse("Missing service client"))
	}

	versionID, err := strconv.ParseInt(c.Param("version_id"), 10, 64)
	if err != nil || versionID <= 0 {
		return c.JSON(http.StatusBadRequest, authErrorResponse("Invalid config version id"))
	}

	if err := h.configs.Acknowledge(c.Request().Context(), versionID, client.Name); err != nil {
		if errors.Is(err, gorm.ErrDuplicatedKey) || isMySQLDuplicateEntry(err) {
			return c.NoContent(http.StatusNoContent)
		}
		return err
	}
	return c.NoContent(http.StatusNoContent)
}

func isMySQLDuplicateEntry(err error) bool {
	var mysqlErr *mysqldriver.MySQLError
	return errors.As(err, &mysqlErr) && mysqlErr.Number == 1062
}
