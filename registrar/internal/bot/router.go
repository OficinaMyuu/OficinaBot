package bot

import (
	"context"
	"log/slog"
	"strings"

	"github.com/bwmarrin/discordgo"
)

type Command interface {
	Execute(context.Context, *CommandContext) error
}

type CommandContext struct {
	Session *discordgo.Session
	Message *discordgo.MessageCreate
	Raw     string
	Args    []string
}

type Router struct {
	prefix   string
	commands map[string]Command
	fallback Command
	logger   *slog.Logger
}

type Route struct {
	Name string
	Args []string
}

func NewRouter(prefix string, logger *slog.Logger) *Router {
	return &Router{
		prefix:   prefix,
		commands: make(map[string]Command),
		logger:   logger,
	}
}

func (r *Router) Register(name string, command Command) {
	r.commands[strings.ToLower(name)] = command
}

func (r *Router) SetFallback(command Command) {
	r.fallback = command
}

func (r *Router) Dispatch(ctx context.Context, session *discordgo.Session, message *discordgo.MessageCreate) bool {
	route, ok := r.Resolve(message.Content)
	if !ok {
		return false
	}

	command := r.fallback
	if route.Name != "" {
		command = r.commands[route.Name]
	}
	if command == nil {
		return false
	}

	go r.execute(ctx, command, &CommandContext{
		Session: session,
		Message: message,
		Raw:     message.Content,
		Args:    route.Args,
	})
	return true
}

func (r *Router) Resolve(raw string) (Route, bool) {
	if !strings.HasPrefix(raw, r.prefix) {
		return Route{}, false
	}

	content := strings.TrimSpace(strings.TrimPrefix(raw, r.prefix))
	if strings.HasPrefix(raw, r.prefix+"revoke") {
		fields := strings.Fields(content)
		if len(fields) == 0 {
			return Route{Name: "revoke"}, true
		}
		if strings.HasPrefix(strings.ToLower(fields[0]), "revoke") {
			fields = fields[1:]
		}
		return Route{Name: "revoke", Args: fields}, true
	}

	return Route{Args: strings.Fields(content)}, true
}

func (r *Router) execute(ctx context.Context, command Command, commandCtx *CommandContext) {
	defer func() {
		if recovered := recover(); recovered != nil {
			r.logger.Error("legacy command panicked", "panic", recovered)
		}
	}()

	if err := command.Execute(ctx, commandCtx); err != nil {
		r.logger.Error("legacy command failed", "error", err)
	}
}
