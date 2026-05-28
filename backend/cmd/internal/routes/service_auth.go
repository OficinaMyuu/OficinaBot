package routes

import (
	"context"
	"errors"
	"net/http"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/auth"
	"oficina-img/internal/repository"
)

const serviceClientContextKey = "service_client"

type ServiceAuthenticator interface {
	Authenticate(ctx context.Context, authorization string) (*repository.BotClient, error)
}

func ServiceAuthMiddleware(authenticator ServiceAuthenticator) echo.MiddlewareFunc {
	return func(next echo.HandlerFunc) echo.HandlerFunc {
		return func(c echo.Context) error {
			client, err := authenticator.Authenticate(c.Request().Context(), c.Request().Header.Get(echo.HeaderAuthorization))
			if err != nil {
				if errors.Is(err, auth.ErrInvalidServiceToken) {
					return c.JSON(http.StatusUnauthorized, authErrorResponse("Invalid service token"))
				}
				return err
			}

			c.Set(serviceClientContextKey, client)
			return next(c)
		}
	}
}

func ServiceClient(c echo.Context) (*repository.BotClient, bool) {
	client, ok := c.Get(serviceClientContextKey).(*repository.BotClient)
	return client, ok
}
