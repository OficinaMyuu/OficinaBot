package handler

import (
	"context"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"encoding/hex"
	"fmt"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/labstack/echo/v4"
	"oficina-img/internal/contract"
	"oficina-img/internal/domain/entity"
	"oficina-img/internal/infrastructure/discord"
)

const (
	dashboardSessionCookie  = "oficina_dashboard_session"
	dashboardStateCookie    = "oficina_dashboard_oauth_state"
	dashboardReturnToCookie = "oficina_dashboard_return_to"
	apiCookiePath           = "/"
	defaultDashboardPath    = "/dashboard"
	sessionTTL              = 12 * time.Hour
	stateTTL                = 10 * time.Minute
	permissionAdministrator = uint64(1 << 3)
	permissionManageGuild   = uint64(1 << 5)
)

type DiscordOAuthClient interface {
	Exchange(ctx context.Context, code, redirectURI string) (string, error)
	CurrentUser(ctx context.Context, accessToken string) (discord.User, error)
	CurrentGuilds(ctx context.Context, accessToken string) ([]discord.Guild, error)
}

type DashboardAuthConfig struct {
	PublicAPIBaseURL   string
	FrontendBaseURL    string
	CORSAllowedOrigins []string
	AuthorizeURL       string
	ClientID           string
	GuildID            string
	CookieSecure       bool
	MissingConfig      []string
}

type DashboardUser = entity.DashboardUser
type DashboardSession = entity.DashboardSession

type SessionRepository interface {
	Save(ctx context.Context, sessionIDHash string, session entity.DashboardSession) error
	Find(ctx context.Context, sessionIDHash string) (entity.DashboardSession, error)
	Delete(ctx context.Context, sessionIDHash string) error
	DeleteExpired(ctx context.Context, now int64) error
}

type SessionStore struct {
	mu         sync.Mutex
	sessions   map[string]DashboardSession
	repository SessionRepository
	now        func() time.Time
}

func NewSessionStore(repositories ...SessionRepository) *SessionStore {
	var repository SessionRepository
	if len(repositories) > 0 {
		repository = repositories[0]
	}
	return &SessionStore{
		sessions:   make(map[string]DashboardSession),
		repository: repository,
		now:        time.Now,
	}
}

func (s *SessionStore) Create(user DashboardUser) (DashboardSession, error) {
	sessionID, err := secureToken()
	if err != nil {
		return DashboardSession{}, err
	}
	csrfToken, err := secureToken()
	if err != nil {
		return DashboardSession{}, err
	}

	session := DashboardSession{
		ID:        sessionID,
		User:      user,
		CSRFToken: csrfToken,
		ExpiresAt: s.now().Add(sessionTTL).Unix(),
	}

	if s.repository != nil {
		if err := s.repository.Save(context.Background(), sessionIDHash(sessionID), session); err != nil {
			return DashboardSession{}, err
		}
		return session, nil
	}

	s.mu.Lock()
	defer s.mu.Unlock()
	s.sessions[sessionID] = session
	return session, nil
}

func (s *SessionStore) Find(sessionID string) (DashboardSession, bool) {
	if s.repository != nil {
		session, err := s.repository.Find(context.Background(), sessionIDHash(sessionID))
		if err != nil {
			return DashboardSession{}, false
		}
		if session.ExpiresAt <= s.now().Unix() {
			_ = s.repository.Delete(context.Background(), sessionIDHash(sessionID))
			return DashboardSession{}, false
		}
		session.ID = sessionID
		_ = s.repository.DeleteExpired(context.Background(), s.now().Unix())
		return session, true
	}

	s.mu.Lock()
	defer s.mu.Unlock()

	session, ok := s.sessions[sessionID]
	if !ok {
		return DashboardSession{}, false
	}
	if session.ExpiresAt <= s.now().Unix() {
		delete(s.sessions, sessionID)
		return DashboardSession{}, false
	}
	return session, true
}

func (s *SessionStore) Delete(sessionID string) {
	if s.repository != nil {
		_ = s.repository.Delete(context.Background(), sessionIDHash(sessionID))
		return
	}

	s.mu.Lock()
	defer s.mu.Unlock()
	delete(s.sessions, sessionID)
}

type DashboardAuthHandler struct {
	cfg      DashboardAuthConfig
	discord  DiscordOAuthClient
	sessions *SessionStore
}

func NewDashboardAuthHandler(cfg DashboardAuthConfig, discordClient DiscordOAuthClient, sessions *SessionStore) *DashboardAuthHandler {
	return &DashboardAuthHandler{
		cfg:      cfg,
		discord:  discordClient,
		sessions: sessions,
	}
}

func (h *DashboardAuthHandler) Login(c echo.Context) error {
	if err := h.ensureConfigured(); err != nil {
		return err
	}

	state, err := secureToken()
	if err != nil {
		return jsonError(c, http.StatusInternalServerError, "Could not start OAuth login")
	}

	returnTo := h.safeReturnTo(c.QueryParam("return_to"))
	setCookie(c, dashboardStateCookie, state, h.cfg.CookieSecure, int(stateTTL.Seconds()))
	setCookie(c, dashboardReturnToCookie, returnTo, h.cfg.CookieSecure, int(stateTTL.Seconds()))

	authorizeURL, err := url.Parse(h.cfg.AuthorizeURL)
	if err != nil {
		return jsonError(c, http.StatusInternalServerError, "Discord authorize URL is invalid")
	}
	query := authorizeURL.Query()
	query.Set("client_id", h.cfg.ClientID)
	query.Set("redirect_uri", h.redirectURI())
	query.Set("response_type", "code")
	query.Set("scope", "identify guilds")
	query.Set("state", state)
	authorizeURL.RawQuery = query.Encode()

	return c.Redirect(http.StatusTemporaryRedirect, authorizeURL.String())
}

func (h *DashboardAuthHandler) Callback(c echo.Context) error {
	if err := h.ensureConfigured(); err != nil {
		return err
	}

	stateCookie, err := c.Cookie(dashboardStateCookie)
	if err != nil || stateCookie.Value == "" || stateCookie.Value != c.QueryParam("state") {
		clearCookie(c, dashboardStateCookie, h.cfg.CookieSecure)
		clearCookie(c, dashboardReturnToCookie, h.cfg.CookieSecure)
		return c.Redirect(http.StatusTemporaryRedirect, h.errorRedirect("state"))
	}
	clearCookie(c, dashboardStateCookie, h.cfg.CookieSecure)
	returnTo := h.returnToFromCookie(c)
	clearCookie(c, dashboardReturnToCookie, h.cfg.CookieSecure)

	code := c.QueryParam("code")
	if code == "" {
		return c.Redirect(http.StatusTemporaryRedirect, h.errorRedirect("oauth"))
	}

	accessToken, err := h.discord.Exchange(c.Request().Context(), code, h.redirectURI())
	if err != nil {
		return c.Redirect(http.StatusTemporaryRedirect, h.errorRedirect("oauth"))
	}

	user, err := h.discord.CurrentUser(c.Request().Context(), accessToken)
	if err != nil {
		return c.Redirect(http.StatusTemporaryRedirect, h.errorRedirect("oauth"))
	}

	guilds, err := h.discord.CurrentGuilds(c.Request().Context(), accessToken)
	if err != nil {
		return c.Redirect(http.StatusTemporaryRedirect, h.errorRedirect("oauth"))
	}

	guild, ok := h.authorizedGuild(guilds)
	if !ok {
		return c.Redirect(http.StatusTemporaryRedirect, h.errorRedirect("forbidden"))
	}

	session, err := h.sessions.Create(DashboardUser{
		ID:           user.ID,
		Username:     user.Username,
		GlobalName:   user.GlobalName,
		AvatarURL:    avatarURL(user),
		GuildName:    guild.Name,
		GuildIconURL: guildIconURL(guild),
		Permissions:  guild.Permissions,
	})
	if err != nil {
		return jsonError(c, http.StatusInternalServerError, "Could not create dashboard session")
	}

	setCookie(c, dashboardSessionCookie, session.ID, h.cfg.CookieSecure, int(sessionTTL.Seconds()))
	return c.Redirect(http.StatusTemporaryRedirect, returnTo)
}

func (h *DashboardAuthHandler) Me(c echo.Context) error {
	if err := h.ensureConfigured(); err != nil {
		return err
	}

	session, ok := h.sessionFromRequest(c)
	if !ok {
		return jsonError(c, http.StatusUnauthorized, "Not authenticated")
	}

	return c.JSON(http.StatusOK, contract.DashboardSessionResponse{
		User:      toDashboardUserResponse(session.User),
		CSRFToken: session.CSRFToken,
	})
}

func (h *DashboardAuthHandler) Logout(c echo.Context) error {
	session, ok := dashboardSession(c)
	if ok {
		h.sessions.Delete(session.ID)
	}
	clearCookie(c, dashboardSessionCookie, h.cfg.CookieSecure)
	return c.NoContent(http.StatusNoContent)
}

func (h *DashboardAuthHandler) RequireSession(next echo.HandlerFunc) echo.HandlerFunc {
	return func(c echo.Context) error {
		if err := h.ensureConfigured(); err != nil {
			return err
		}

		session, ok := h.sessionFromRequest(c)
		if !ok {
			return jsonError(c, http.StatusUnauthorized, "Not authenticated")
		}

		if isMutatingMethod(c.Request().Method) && c.Request().Header.Get("X-CSRF-Token") != session.CSRFToken {
			return jsonError(c, http.StatusForbidden, "Invalid CSRF token")
		}

		c.Set("dashboardSession", session)
		return next(c)
	}
}

func (h *DashboardAuthHandler) ensureConfigured() error {
	if len(h.cfg.MissingConfig) > 0 || h.discord == nil || h.sessions == nil {
		missing := h.cfg.MissingConfig
		if len(missing) == 0 {
			missing = []string{"dashboard runtime"}
		}
		return echo.NewHTTPError(http.StatusServiceUnavailable, fmt.Sprintf("Dashboard API is missing configuration: %s", strings.Join(missing, ", ")))
	}
	return nil
}

func (h *DashboardAuthHandler) redirectURI() string {
	return h.cfg.PublicAPIBaseURL + "/auth/discord/callback"
}

func (h *DashboardAuthHandler) errorRedirect(code string) string {
	loginURL, err := url.Parse(h.cfg.FrontendBaseURL + defaultDashboardPath + "/login")
	if err != nil {
		return defaultDashboardPath + "/login?error=" + url.QueryEscape(code)
	}
	query := loginURL.Query()
	query.Set("error", code)
	loginURL.RawQuery = query.Encode()
	return loginURL.String()
}

func (h *DashboardAuthHandler) returnToFromCookie(c echo.Context) string {
	cookie, err := c.Cookie(dashboardReturnToCookie)
	if err != nil {
		return h.defaultReturnTo()
	}
	return h.safeReturnTo(cookie.Value)
}

func (h *DashboardAuthHandler) safeReturnTo(raw string) string {
	if raw == "" {
		return h.defaultReturnTo()
	}
	value, err := url.Parse(raw)
	if err != nil || !value.IsAbs() {
		return h.defaultReturnTo()
	}
	if !h.isAllowedReturnOrigin(value) {
		return h.defaultReturnTo()
	}
	if value.Path != defaultDashboardPath && !strings.HasPrefix(value.Path, defaultDashboardPath+"/") {
		return h.defaultReturnTo()
	}
	value.Fragment = ""
	return value.String()
}

func (h *DashboardAuthHandler) isAllowedReturnOrigin(value *url.URL) bool {
	if value.Scheme == "" || value.Host == "" {
		return false
	}
	origin := strings.ToLower(value.Scheme) + "://" + strings.ToLower(value.Host)
	if frontend, err := url.Parse(h.cfg.FrontendBaseURL); err == nil && frontend.Scheme != "" && frontend.Host != "" {
		if origin == strings.ToLower(frontend.Scheme)+"://"+strings.ToLower(frontend.Host) {
			return true
		}
	}
	for _, allowed := range h.cfg.CORSAllowedOrigins {
		if origin == strings.ToLower(strings.TrimRight(allowed, "/")) {
			return true
		}
	}
	return false
}

func (h *DashboardAuthHandler) defaultReturnTo() string {
	return h.cfg.FrontendBaseURL + defaultDashboardPath
}

func (h *DashboardAuthHandler) authorizedGuild(guilds []discord.Guild) (discord.Guild, bool) {
	for _, guild := range guilds {
		if guild.ID != h.cfg.GuildID {
			continue
		}
		if guild.Owner {
			return guild, true
		}
		permissions, err := strconv.ParseUint(guild.Permissions, 10, 64)
		if err != nil {
			return discord.Guild{}, false
		}
		if permissions&permissionAdministrator != 0 || permissions&permissionManageGuild != 0 {
			return guild, true
		}
		return discord.Guild{}, false
	}
	return discord.Guild{}, false
}

func (h *DashboardAuthHandler) sessionFromRequest(c echo.Context) (DashboardSession, bool) {
	cookie, err := c.Cookie(dashboardSessionCookie)
	if err != nil || cookie.Value == "" {
		return DashboardSession{}, false
	}
	return h.sessions.Find(cookie.Value)
}

func dashboardSession(c echo.Context) (DashboardSession, bool) {
	session, ok := c.Get("dashboardSession").(DashboardSession)
	return session, ok
}

func setCookie(c echo.Context, name, value string, secure bool, maxAge int) {
	c.SetCookie(&http.Cookie{
		Name:     name,
		Value:    value,
		Path:     apiCookiePath,
		MaxAge:   maxAge,
		HttpOnly: true,
		Secure:   secure,
		SameSite: http.SameSiteLaxMode,
	})
}

func clearCookie(c echo.Context, name string, secure bool) {
	setCookie(c, name, "", secure, -1)
}

func secureToken() (string, error) {
	bytes := make([]byte, 32)
	if _, err := rand.Read(bytes); err != nil {
		return "", err
	}
	return base64.RawURLEncoding.EncodeToString(bytes), nil
}

func sessionIDHash(sessionID string) string {
	hash := sha256.Sum256([]byte(sessionID))
	return hex.EncodeToString(hash[:])
}

func toDashboardUserResponse(user DashboardUser) contract.DashboardUserResponse {
	return contract.DashboardUserResponse{
		ID:           user.ID,
		Username:     user.Username,
		GlobalName:   user.GlobalName,
		AvatarURL:    user.AvatarURL,
		GuildName:    user.GuildName,
		GuildIconURL: user.GuildIconURL,
		Permissions:  user.Permissions,
	}
}

func avatarURL(user discord.User) *string {
	if user.Avatar == nil || *user.Avatar == "" {
		return nil
	}
	value := fmt.Sprintf("https://cdn.discordapp.com/avatars/%s/%s.png", user.ID, *user.Avatar)
	return &value
}

func guildIconURL(guild discord.Guild) *string {
	if guild.Icon == nil || *guild.Icon == "" {
		return nil
	}
	value := fmt.Sprintf("https://cdn.discordapp.com/icons/%s/%s.png", guild.ID, *guild.Icon)
	return &value
}

func isMutatingMethod(method string) bool {
	switch method {
	case http.MethodPost, http.MethodPut, http.MethodPatch, http.MethodDelete:
		return true
	default:
		return false
	}
}
