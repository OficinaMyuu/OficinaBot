package auth

import (
	"context"
	"errors"
	"testing"
	"time"

	"golang.org/x/oauth2"
	"gorm.io/gorm"
	"oficina-img/internal/repository"
)

func TestStartURLReturnsSignedState(t *testing.T) {
	service := newTestService()

	url, state, err := service.StartURL()
	if err != nil {
		t.Fatalf("start url: %v", err)
	}
	if url != "https://discord.test/oauth?state="+state {
		t.Fatalf("unexpected auth url %q", url)
	}
	if !service.validateState(state) {
		t.Fatal("expected generated state to validate")
	}
}

func TestCompleteOAuthBootstrapsOwner(t *testing.T) {
	service := newTestService()
	_, state, err := service.StartURL()
	if err != nil {
		t.Fatalf("start url: %v", err)
	}

	sessionToken, user, err := service.CompleteOAuth(context.Background(), "code", state, state)
	if err != nil {
		t.Fatalf("complete oauth: %v", err)
	}

	if user.DiscordID != "owner" {
		t.Fatalf("expected owner user, got %+v", user)
	}
	if sessionToken == "" {
		t.Fatal("expected session token")
	}
	if _, ok := service.sessions.(*memorySessionStore).sessions[HashToken(sessionToken)]; !ok {
		t.Fatal("expected hashed session to be stored")
	}
}

func TestCompleteOAuthRejectsInvalidState(t *testing.T) {
	service := newTestService()

	if _, _, err := service.CompleteOAuth(context.Background(), "code", "invalid", "expected"); !errors.Is(err, ErrInvalidState) {
		t.Fatalf("expected invalid state error, got %v", err)
	}
}

func TestCompleteOAuthRejectsNonAllowlistedAdmin(t *testing.T) {
	service := newTestService()
	service.discord.(*fakeDiscordClient).user = &DiscordUser{ID: "stranger", Username: "Nope"}
	_, state, err := service.StartURL()
	if err != nil {
		t.Fatalf("start url: %v", err)
	}

	if _, _, err := service.CompleteOAuth(context.Background(), "code", state, state); !errors.Is(err, ErrUnauthorizedAdmin) {
		t.Fatalf("expected unauthorized admin error, got %v", err)
	}
}

func TestAdminManagementRequiresOwner(t *testing.T) {
	service := newTestService()
	actor := &repository.User{DiscordID: "not-owner", Username: "Staff"}

	if _, err := service.AddAdmin(context.Background(), actor, "200", "Admin"); !errors.Is(err, ErrOwnerOnlyOperation) {
		t.Fatalf("expected owner-only add error, got %v", err)
	}
	if err := service.RemoveAdmin(context.Background(), actor, "200"); !errors.Is(err, ErrOwnerOnlyOperation) {
		t.Fatalf("expected owner-only remove error, got %v", err)
	}
}

func TestAdminManagementPreventsOwnerRemoval(t *testing.T) {
	service := newTestService()
	actor := &repository.User{DiscordID: "owner", Username: "Leonardo"}

	if err := service.RemoveAdmin(context.Background(), actor, "owner"); !errors.Is(err, ErrOwnerRemoval) {
		t.Fatalf("expected owner removal error, got %v", err)
	}
}

func newTestService() *Service {
	service := NewService(
		&fakeDiscordClient{user: &DiscordUser{ID: "owner", Username: "Leonardo"}},
		newMemoryUserStore(),
		newMemorySessionStore(),
		Config{
			OwnerDiscordID: "owner",
			SessionSecret:  "secret",
			SessionTTL:     time.Hour,
		},
	)
	service.now = func() time.Time { return time.Unix(100, 0).UTC() }
	service.randomBytes = func(length int) ([]byte, error) {
		buf := make([]byte, length)
		for i := range buf {
			buf[i] = byte(i + 1)
		}
		return buf, nil
	}
	return service
}

type fakeDiscordClient struct {
	user *DiscordUser
}

func (f *fakeDiscordClient) AuthCodeURL(state string) string {
	return "https://discord.test/oauth?state=" + state
}

func (f *fakeDiscordClient) Exchange(_ context.Context, _ string) (*oauth2.Token, error) {
	return &oauth2.Token{AccessToken: "token"}, nil
}

func (f *fakeDiscordClient) CurrentUser(_ context.Context, _ *oauth2.Token) (*DiscordUser, error) {
	return f.user, nil
}

type memoryUserStore struct {
	users map[string]*repository.User
}

func newMemoryUserStore() *memoryUserStore {
	return &memoryUserStore{users: map[string]*repository.User{}}
}

func (s *memoryUserStore) Create(_ context.Context, user *repository.User) error {
	copy := *user
	s.users[user.DiscordID] = &copy
	return nil
}

func (s *memoryUserStore) Get(_ context.Context, discordID string) (*repository.User, error) {
	user, ok := s.users[discordID]
	if !ok {
		return nil, gorm.ErrRecordNotFound
	}
	copy := *user
	return &copy, nil
}

func (s *memoryUserStore) List(_ context.Context) ([]repository.User, error) {
	users := make([]repository.User, 0, len(s.users))
	for _, user := range s.users {
		users = append(users, *user)
	}
	return users, nil
}

func (s *memoryUserStore) Delete(_ context.Context, discordID string) error {
	delete(s.users, discordID)
	return nil
}

type memorySessionStore struct {
	sessions map[string]*repository.AdminSession
}

func newMemorySessionStore() *memorySessionStore {
	return &memorySessionStore{sessions: map[string]*repository.AdminSession{}}
}

func (s *memorySessionStore) Create(_ context.Context, session *repository.AdminSession) error {
	copy := *session
	s.sessions[session.TokenHash] = &copy
	return nil
}

func (s *memorySessionStore) GetValid(_ context.Context, tokenHash string, now time.Time) (*repository.AdminSession, error) {
	session, ok := s.sessions[tokenHash]
	if !ok || !session.ExpiresAt.After(now) {
		return nil, gorm.ErrRecordNotFound
	}
	copy := *session
	copy.User = repository.User{DiscordID: copy.DiscordID, Username: "Leonardo"}
	return &copy, nil
}

func (s *memorySessionStore) Touch(_ context.Context, tokenHash string, seenAt time.Time) error {
	s.sessions[tokenHash].LastSeenAt = seenAt
	return nil
}

func (s *memorySessionStore) Delete(_ context.Context, tokenHash string) error {
	delete(s.sessions, tokenHash)
	return nil
}
