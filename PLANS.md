# OficinaServices Implementation Plan

## Summary

Build an admin dashboard for OficinaServices with:

- `backend/`: Go + Echo API on AWS EC2.
- `frontend/`: React + Vite dashboard with a Discord-like, purple-oriented UI.
- Existing bots remain on Hostsquare/Pterodactyl and sync with the backend through authenticated HTTP APIs.
- Backend work starts with refactoring the current Go service for readability, lifecycle safety, testability, and separation of concerns before adding dashboard features.

## Architecture Decisions

- Use SQLite with WAL mode on EC2 for the backend database.
- Use Discord OAuth2 for admin login, restricted to allowlisted Discord user IDs.
- Do not use weekly shared passcodes for human admins. That idea is convenient, but it is basically “a sticky note with TLS.” Fine for emergencies, bad as the main auth model.
- Use separate service tokens for bots, sent as `Authorization: Bearer ...`, hashed at rest.
- Do not make `/configs/diff` destructive. Bots should poll versioned config changes and ACK what they applied, so failed requests do not silently lose configuration updates.
- Use `discordgo` only for REST calls from the backend. Do not connect the backend to the Discord gateway/websocket.

## Backend Plan

1. Refactor the existing backend foundation.

   - Keep Echo, but move setup out of `backend/cmd/api/main.go`.
   - Split config loading, router setup, middleware, service initialization, and graceful shutdown.
   - Replace global Playwright state with injected services and explicit cleanup.
   - Separate DTOs, handlers, validators, rendering, and external video download logic.
   - Add focused tests around validation, routing behavior, and service errors.

2. Add backend persistence.

   - Add SQLite database initialization with WAL mode.
   - Add migrations using `pressly/goose`.
   - Add repository interfaces and implementations around `database/sql`.
   - Create tables for admin users, bot clients, ingested event batches, message logs, punishments, config versions, config acknowledgements, and audit actions.

3. Add admin authentication.

   - Implement Discord OAuth2 login using the `identify` scope.
   - Store allowlisted admins in a `users` table.
   - Allow only the configured owner Discord ID to register or remove admin users.
   - Use secure HttpOnly session cookies for the frontend.
   - Do not add roles or a permission system in v1.

4. Add bot/service authentication.

   - Create service clients for `bot` and `registrar`.
   - Store only hashed service tokens.
   - Require bearer auth for bot-to-backend endpoints.
   - Add request IDs, structured logs, body size limits, CORS, CSRF protection for session APIs, and basic rate limits.

5. Add dashboard APIs.

   - Add batch ingestion endpoints for logs, registrations, punishments, moderation events, and sync health.
   - Add read endpoints for dashboard tables and detail views.
   - Add write endpoints for bad words and automod-related configuration.
   - Store desired config as versioned state.
   - Add bot polling endpoints that return unapplied config versions and require explicit ACK from the bot.

6. Add Discord REST integration.

   - Add a small `discordgo` wrapper for REST-only calls.
   - Support fetching guild, channel, role, and user metadata needed by the dashboard.
   - Cache Discord metadata where useful to avoid unnecessary API calls.
   - Keep this layer independent from websocket/event handling.

## Frontend Plan

7. Scaffold the frontend.

   - Add `frontend/` using React, Vite, TypeScript, and a small API client.
   - Use React Router for pages.
   - Use TanStack Query or an equivalent request/cache library.
   - Configure linting, formatting, and tests.

8. Build the UI system.

   - Use a Discord-inspired dark palette shifted toward purple.
   - Keep the layout app-like, not landing-page-like.
   - Add sidebar navigation, top context bar, tables, filters, forms, modals, toasts, empty states, loading states, and error states.
   - Prefer dense admin UI over decorative marketing sections.

9. Add authentication screens.

   - Add Discord login flow.
   - Add logged-out, loading-session, forbidden, and authenticated states.
   - Add a simple admin user management page visible only to the owner.

10. Add dashboard views.

   - Overview: service health, recent syncs, recent moderation actions.
   - Logs: message logs with filtering and pagination.
   - Punishments: punishment history and detail view.
   - Registrations: registered members and recent registration events.
   - Config: bad words and automod settings.

11. Add config management UX.

   - Show pending/applied config versions.
   - Show which bot client last ACKed a config version.
   - Make it clear that some changes can take up to 5 minutes to apply.
   - Add audit trail entries for admin changes.

## Bot And Registrar Integration Plan

- Keep each bot’s local database as the source of truth for its current runtime behavior.
- Add periodic POST sync jobs for logs and events.
- Add config polling in the Oficina bot for automod-related config.
- Add ACK behavior after the bot successfully applies a config version.
- Avoid requiring bots to synchronously call the backend for latency-sensitive moderation decisions.

## Test Plan

- Backend unit tests for services, validators, repositories, and auth helpers.
- Backend handler tests using Echo test contexts.
- Backend integration tests against a real temporary SQLite database with migrations applied.
- Frontend component tests for auth gates, tables, forms, and config screens.
- API client tests for success, validation errors, forbidden responses, and expired sessions.
- Manual smoke test: login, view dashboard, edit bad words, bot polls config, bot ACKs applied version.
- Existing service validation remains required:
  - Bot: `mvn "-Dmaven.repo.local=../.m2" test` from `bot/`.
  - Registrar: `mvn clean package` from `registrar/`.
  - Backend: `go test ./...` from `backend/cmd/`.

## Documentation Plan

- Keep root-only documentation.
- Update `ARCHITECTURE.md` when backend/frontend architecture lands.
- Update `AGENTS.md` when new entrypoints, test commands, or workflow ownership changes.
- Use this `PLANS.md` as the implementation checklist until the dashboard work is complete.