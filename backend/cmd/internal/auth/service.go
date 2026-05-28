package auth

import (
	"context"
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"encoding/hex"
	"errors"
	"fmt"
	"strings"
	"time"

	"golang.org/x/oauth2"
	"gorm.io/gorm"
	"oficina-img/internal/repository"
)

const DiscordIdentifyScope = "identify"

var (
	ErrInvalidState       = errors.New("invalid oauth state")
	ErrUnauthorizedAdmin  = errors.New("discord user is not an allowlisted admin")
	ErrOwnerRemoval       = errors.New("owner admin cannot be removed")
	ErrOwnerOnlyOperation = errors.New("only the owner can manage admins")
)

type DiscordUser struct {
	ID       string
	Username string
}

type DiscordOAuthClient interface {
	AuthCodeURL(state string) string
	Exchange(ctx context.Context, code string) (*oauth2.Token, error)
	CurrentUser(ctx context.Context, token *oauth2.Token) (*DiscordUser, error)
}

type UserStore interface {
	Create(ctx context.Context, user *repository.User) error
	Get(ctx context.Context, discordID string) (*repository.User, error)
	List(ctx context.Context) ([]repository.User, error)
	Delete(ctx context.Context, discordID string) error
}

type SessionStore interface {
	Create(ctx context.Context, session *repository.AdminSession) error
	GetValid(ctx context.Context, tokenHash string, now time.Time) (*repository.AdminSession, error)
	Touch(ctx context.Context, tokenHash string, seenAt time.Time) error
	Delete(ctx context.Context, tokenHash string) error
}

type Service struct {
	discord        DiscordOAuthClient
	users          UserStore
	sessions       SessionStore
	ownerDiscordID string
	sessionSecret  []byte
	sessionTTL     time.Duration
	now            func() time.Time
	randomBytes    func(int) ([]byte, error)
}

type Config struct {
	OwnerDiscordID string
	SessionSecret  string
	SessionTTL     time.Duration
}

func NewService(discord DiscordOAuthClient, users UserStore, sessions SessionStore, cfg Config) *Service {
	return &Service{
		discord:        discord,
		users:          users,
		sessions:       sessions,
		ownerDiscordID: cfg.OwnerDiscordID,
		sessionSecret:  []byte(cfg.SessionSecret),
		sessionTTL:     cfg.SessionTTL,
		now:            func() time.Time { return time.Now().UTC() },
		randomBytes:    secureRandomBytes,
	}
}

func (s *Service) StartURL() (string, string, error) {
	state, err := s.newState()
	if err != nil {
		return "", "", err
	}
	return s.discord.AuthCodeURL(state), state, nil
}

func (s *Service) CompleteOAuth(ctx context.Context, code, state, expectedState string) (string, *repository.User, error) {
	if !constantTimeEqual(state, expectedState) || !s.validateState(state) {
		return "", nil, ErrInvalidState
	}

	token, err := s.discord.Exchange(ctx, code)
	if err != nil {
		return "", nil, fmt.Errorf("exchange discord oauth code: %w", err)
	}

	discordUser, err := s.discord.CurrentUser(ctx, token)
	if err != nil {
		return "", nil, fmt.Errorf("fetch discord user: %w", err)
	}

	user, err := s.ensureAllowedUser(ctx, discordUser)
	if err != nil {
		return "", nil, err
	}

	sessionToken, sessionHash, err := s.newSessionToken()
	if err != nil {
		return "", nil, err
	}
	if err := s.sessions.Create(ctx, &repository.AdminSession{
		TokenHash: sessionHash,
		DiscordID: discordUser.ID,
		ExpiresAt: s.now().Add(s.sessionTTL),
	}); err != nil {
		return "", nil, err
	}

	return sessionToken, user, nil
}

func (s *Service) CurrentUser(ctx context.Context, sessionToken string) (*repository.User, error) {
	session, err := s.sessions.GetValid(ctx, HashToken(sessionToken), s.now())
	if err != nil {
		return nil, err
	}
	if err := s.sessions.Touch(ctx, session.TokenHash, s.now()); err != nil {
		return nil, err
	}
	return &session.User, nil
}

func (s *Service) Logout(ctx context.Context, sessionToken string) error {
	if strings.TrimSpace(sessionToken) == "" {
		return nil
	}
	return s.sessions.Delete(ctx, HashToken(sessionToken))
}

func (s *Service) ListAdmins(ctx context.Context, actor *repository.User) ([]repository.User, error) {
	if !s.IsOwner(actor) {
		return nil, ErrOwnerOnlyOperation
	}
	return s.users.List(ctx)
}

func (s *Service) AddAdmin(ctx context.Context, actor *repository.User, discordID, username string) (*repository.User, error) {
	if !s.IsOwner(actor) {
		return nil, ErrOwnerOnlyOperation
	}

	user := &repository.User{
		DiscordID: strings.TrimSpace(discordID),
		Username:  strings.TrimSpace(username),
	}
	if err := s.users.Create(ctx, user); err != nil {
		return nil, err
	}
	return user, nil
}

func (s *Service) RemoveAdmin(ctx context.Context, actor *repository.User, discordID string) error {
	if !s.IsOwner(actor) {
		return ErrOwnerOnlyOperation
	}
	if discordID == s.ownerDiscordID {
		return ErrOwnerRemoval
	}
	return s.users.Delete(ctx, discordID)
}

func (s *Service) IsOwner(user *repository.User) bool {
	return user != nil && user.DiscordID == s.ownerDiscordID
}

func (s *Service) ensureAllowedUser(ctx context.Context, discordUser *DiscordUser) (*repository.User, error) {
	user, err := s.users.Get(ctx, discordUser.ID)
	if err == nil {
		return user, nil
	}
	if !errors.Is(err, gorm.ErrRecordNotFound) {
		return nil, err
	}
	if discordUser.ID != s.ownerDiscordID {
		return nil, ErrUnauthorizedAdmin
	}

	user = &repository.User{
		DiscordID: discordUser.ID,
		Username:  discordUser.Username,
	}
	if err := s.users.Create(ctx, user); err != nil {
		return nil, err
	}
	return user, nil
}

func (s *Service) newState() (string, error) {
	raw, err := s.randomBytes(32)
	if err != nil {
		return "", err
	}
	mac := hmac.New(sha256.New, s.sessionSecret)
	mac.Write(raw)

	value := base64.RawURLEncoding.EncodeToString(raw)
	signature := base64.RawURLEncoding.EncodeToString(mac.Sum(nil))
	return value + "." + signature, nil
}

func (s *Service) newSessionToken() (string, string, error) {
	raw, err := s.randomBytes(32)
	if err != nil {
		return "", "", err
	}
	token := base64.RawURLEncoding.EncodeToString(raw)
	return token, HashToken(token), nil
}

func (s *Service) validateState(state string) bool {
	parts := strings.Split(state, ".")
	if len(parts) != 2 {
		return false
	}

	raw, err := base64.RawURLEncoding.DecodeString(parts[0])
	if err != nil {
		return false
	}
	signature, err := base64.RawURLEncoding.DecodeString(parts[1])
	if err != nil {
		return false
	}

	mac := hmac.New(sha256.New, s.sessionSecret)
	mac.Write(raw)
	return hmac.Equal(signature, mac.Sum(nil))
}

func HashToken(token string) string {
	sum := sha256.Sum256([]byte(token))
	return hex.EncodeToString(sum[:])
}

func constantTimeEqual(got, want string) bool {
	if got == "" || want == "" {
		return false
	}
	return hmac.Equal([]byte(got), []byte(want))
}

func secureRandomBytes(length int) ([]byte, error) {
	buf := make([]byte, length)
	if _, err := rand.Read(buf); err != nil {
		return nil, err
	}
	return buf, nil
}
