# AGENTS.md

## Purpose
This is the root index for the OficinaServices mono-repo. Read this file first, then open the service-specific documentation for the area you are changing.

## Layout
- `bot/` contains the Oficina Discord bot, formerly `OficinaMyuu/OficinaBot`.
- `backend/` contains the Go backend, formerly `OficinaMyuu/OficinaImagery`.
- `registrar/` contains the registration Discord service, formerly `OficinaMyuu/RegistroOficina`.
- `.github/workflows/` contains repo-level CI/deploy workflows. Keep workflows at the repository root so GitHub Actions can discover them.

## Hard Rules For Exploration
- Never read local artifacts such as `database.db`, `database-backup.db`, `bot.jar`, files inside `target/`, generated binaries, or local backups.
- Do not inspect `bot/content/*.json` unless the task is explicitly about content payloads. These are environment-specific data files.
- Prefer service entrypoints and registration files before feature implementations.
- Keep changes scoped to the relevant service unless the task is explicitly cross-service.

## Service Entry Points
- Bot: start with `bot/AGENTS.md`, then `bot/src/main/java/ofc/bot/Main.java`.
- Backend: start with `backend/cmd/api/main.go`; routes live under `backend/cmd/internal/routes/`.
- Registrar: start with `registrar/src/main/java/ofc/bot/RegisterMaster.java`.

## Builds And Tests
- Bot tests: run `mvn "-Dmaven.repo.local=.m2" test` from `bot/`.
- Bot package: run `mvn clean package` from `bot/`.
- Registrar package: run `mvn clean package` from `registrar/`.
- Backend build/test commands should be run from `backend/cmd/` unless the backend structure changes.

## Deployments
- Bot deploy workflow: `.github/workflows/deploy.yml`.
- Registrar deploy workflow: `.github/workflows/deploy-registrar.yml`.
- Backend deployment is intentionally not wired at the mono-repo root yet.
- Bot deployment secrets are service-scoped with the `OFICINA` segment, such as `SFTP_OFICINA_HOST` and `PTERO_OFICINA_SERVER_ID`.
- Registrar deployment secrets are service-scoped with the `REGISTRY` segment, such as `SFTP_REGISTRY_HOST` and `PTERO_REGISTRY_SERVER_ID`.
- `PTERO_API_KEY` remains shared unless a future deployment split requires service-specific API keys.

## Documentation
- Update the root docs when changing mono-repo structure, shared workflows, or cross-service conventions.
- Update service docs when changing behavior inside a service.
