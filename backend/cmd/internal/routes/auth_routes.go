package routes

import (
	"context"
	"errors"
	"net/http"
	"time"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/auth"
	"oficina-img/internal/repository"
)

const oauthStateCookieName = "oficina_oauth_state"

type AdminAuthService interface {
	StartURL() (string, string, error)
	CompleteOAuth(ctx context.Context, code, state, expectedState string) (string, *repository.User, error)
	CurrentUser(ctx context.Context, sessionToken string) (*repository.User, error)
	Logout(ctx context.Context, sessionToken string) error
	IsOwner(user *repository.User) bool
}

type AuthCookieConfig struct {
	SessionName         string
	Secure              bool
	FrontendRedirectURL string
}

type AuthHandler struct {
	auth    AdminAuthService
	cookies AuthCookieConfig
}

func NewAuthHandler(authService AdminAuthService, cookies AuthCookieConfig) *AuthHandler {
	return &AuthHandler{auth: authService, cookies: cookies}
}

func (h *AuthHandler) StartDiscordLogin(c echo.Context) error {
	url, state, err := h.auth.StartURL()
	if err != nil {
		return err
	}

	c.SetCookie(h.cookie(oauthStateCookieName, state, 10*time.Minute))
	return c.Redirect(http.StatusFound, url)
}

func (h *AuthHandler) CompleteDiscordLogin(c echo.Context) error {
	stateCookie, err := c.Cookie(oauthStateCookieName)
	if err != nil {
		return c.JSON(http.StatusBadRequest, authErrorResponse("Missing OAuth state cookie"))
	}

	sessionToken, _, err := h.auth.CompleteOAuth(c.Request().Context(), c.QueryParam("code"), c.QueryParam("state"), stateCookie.Value)
	if err != nil {
		return writeAuthError(c, err)
	}

	c.SetCookie(h.expiredCookie(oauthStateCookieName))
	c.SetCookie(h.cookie(h.cookies.SessionName, sessionToken, 7*24*time.Hour))
	return c.Redirect(http.StatusFound, h.cookies.FrontendRedirectURL)
}

func (h *AuthHandler) CurrentUser(c echo.Context) error {
	sessionCookie, err := c.Cookie(h.cookies.SessionName)
	if err != nil {
		return c.JSON(http.StatusUnauthorized, authErrorResponse("Missing admin session"))
	}

	user, err := h.auth.CurrentUser(c.Request().Context(), sessionCookie.Value)
	if err != nil {
		return c.JSON(http.StatusUnauthorized, authErrorResponse("Invalid admin session"))
	}

	return c.JSON(http.StatusOK, userResponse{
		DiscordID: user.DiscordID,
		Username:  user.Username,
		IsOwner:   h.auth.IsOwner(user),
	})
}

func (h *AuthHandler) Logout(c echo.Context) error {
	sessionCookie, err := c.Cookie(h.cookies.SessionName)
	if err == nil {
		if err := h.auth.Logout(c.Request().Context(), sessionCookie.Value); err != nil {
			return err
		}
	}

	c.SetCookie(h.expiredCookie(h.cookies.SessionName))
	return c.NoContent(http.StatusNoContent)
}

func (h *AuthHandler) cookie(name, value string, ttl time.Duration) *http.Cookie {
	return &http.Cookie{
		Name:     name,
		Value:    value,
		Path:     "/",
		MaxAge:   int(ttl.Seconds()),
		Expires:  time.Now().Add(ttl),
		HttpOnly: true,
		Secure:   h.cookies.Secure,
		SameSite: http.SameSiteLaxMode,
	}
}

func (h *AuthHandler) expiredCookie(name string) *http.Cookie {
	return &http.Cookie{
		Name:     name,
		Value:    "",
		Path:     "/",
		MaxAge:   -1,
		Expires:  time.Unix(0, 0),
		HttpOnly: true,
		Secure:   h.cookies.Secure,
		SameSite: http.SameSiteLaxMode,
	}
}

type userResponse struct {
	DiscordID string `json:"discord_id"`
	Username  string `json:"username"`
	IsOwner   bool   `json:"is_owner"`
}

type authError struct {
	Message string `json:"message"`
}

func authErrorResponse(message string) authError {
	return authError{Message: message}
}

func writeAuthError(c echo.Context, err error) error {
	switch {
	case errors.Is(err, auth.ErrInvalidState):
		return c.JSON(http.StatusBadRequest, authErrorResponse("Invalid OAuth state"))
	case errors.Is(err, auth.ErrUnauthorizedAdmin):
		return c.JSON(http.StatusForbidden, authErrorResponse("Discord user is not an allowlisted admin"))
	default:
		return err
	}
}
