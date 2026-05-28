package routes

import (
	"net/http"
	"time"

	"github.com/labstack/echo/v4"
)

type ServiceHandler struct{}

func NewServiceHandler() *ServiceHandler {
	return &ServiceHandler{}
}

func (h *ServiceHandler) Me(c echo.Context) error {
	client, ok := ServiceClient(c)
	if !ok {
		return c.JSON(http.StatusUnauthorized, authErrorResponse("Missing service client"))
	}

	return c.JSON(http.StatusOK, serviceClientResponse{
		Name:       client.Name,
		LastSeenAt: client.LastSeenAt,
	})
}

type serviceClientResponse struct {
	Name       string     `json:"name"`
	LastSeenAt *time.Time `json:"last_seen_at"`
}
