# ARCHITECTURE.md

## Overview
OficinaServices is the mono-repo for Oficina's Discord-facing services and shared backend work. The repository currently contains three independently built services with separate runtime responsibilities.

## Services
- `bot/`: Java 21 Discord bot using JDA 6, Maven, SQLite, jOOQ, HikariCP, Quartz, OkHttp, and the OpenAI Java SDK. Service architecture is documented in `bot/ARCHITECTURE.md`.
- `backend/`: Go HTTP backend that started as OficinaImagery and is expected to take on broader API responsibilities. Its current entrypoint is `backend/cmd/api/main.go`.
- `registrar/`: Java 17 Discord registration service using JDA 5 and Maven. Its entrypoint is `registrar/src/main/java/ofc/bot/RegisterMaster.java`.

## Repository Structure
- Repo-level GitHub Actions workflows live in `.github/workflows/`.
- Service source, build descriptors, service docs, and service-owned assets live inside each service directory.
- Runtime files, generated artifacts, local databases, and package outputs are ignored and are not source of truth.

## Deployment Model
- The bot workflow builds `bot/target/bot.jar`, uploads it through the Oficina SFTP secrets, and restarts `PTERO_OFICINA_SERVER_ID`.
- The registrar workflow builds `registrar/target/bot.jar`, uploads it through the Registry SFTP secrets, and restarts `PTERO_REGISTRY_SERVER_ID`.
- Backend deployment is intentionally left undefined while the backend responsibilities are expanded.

## History Preservation
This mono-repo was assembled with history-preserving subtree imports:
- `OficinaMyuu/OficinaImagery` was imported under `backend/`.
- `OficinaMyuu/RegistroOficina` was imported under `registrar/`.

Use non-squashed subtree-style merges for future imports that must preserve source repository history.
