# Oficina Ansible

This directory manages the Docker runtime layer for the OficinaServices VMs.

## Topology

- `bots` group: public bots VM, reachable over SSH as `ubuntu`. It runs the `bot`, any additional bot containers defined in the inventory, and `registrar`.
- `backend` group: backend/API VM, reachable directly over SSH as `ubuntu` from the configured admin CIDR. It runs the `backend` container.
- Both hosts run Docker Engine, Docker Compose v2, and one Watchtower container.
- Watchtower polls every 300 seconds and updates all containers on the host. The runtime role sets `DOCKER_API_VERSION` from `oficina_watchtower_docker_api_version` so Watchtower stays compatible with newer Docker daemons.

Both VMs accept SSH only from the configured admin source CIDR, so Ansible connects directly:

```text
local Ansible -> ubuntu@bots-public-ip
local Ansible -> ubuntu@backend-public-ip
```

The private key stays on the local operator machine and must match the public key Terraform placed in both VMs.

## Registry Model

GitHub Actions builds images and pushes them to GHCR:

- `ghcr.io/<owner>/oficina-bot:latest`
- `ghcr.io/<owner>/oficina-registrar:latest`
- `ghcr.io/<owner>/oficina-backend:latest`

The VMs do not need `git` to deploy those images. They need Docker, DNS, outbound HTTPS access to GHCR, and registry credentials only when the GHCR packages are private.

If the backend VM is later moved behind a bastion or into a subnet without direct internet egress, update the inventory with `ansible_ssh_common_args`/`ProxyJump` and add a NAT path or registry mirror before expecting Docker or Watchtower pulls to work.

## Usage

Run the Ansible commands from the repository root. A root `ansible.cfg` is
provided for normal filesystems; the playbook also references local roles by
path so WSL can still run it from `/mnt/c` when Ansible ignores config files in
world-writable Windows mounts.

Install the collection dependencies:

```sh
ansible-galaxy collection install -r ansible/requirements.yml
```

Create a private inventory from the example:

```sh
cp -R ansible/inventories/example ansible/inventories/prod
```

Then edit `ansible/inventories/prod/hosts.yml`:

- Set `bots-01.ansible_host` to the bots VM public IP or DNS name.
- Set `backend-01.ansible_host` to the backend VM public IP or DNS name.
- Set `ansible_ssh_private_key_file` to the local private key matching the Terraform `ssh_public_key`.

Edit private group vars:

- Set `oficina_image_namespace` to the lowercase GHCR owner.
- Set `oficina_registry_username` and `oficina_registry_token` only if GHCR packages are private.
- Put service secrets in private vars or Ansible Vault, not in committed example files.
- Service `.env` files are mounted through Docker Compose with `env_file.format: raw`.
  Keep this raw format when secrets contain `$`, because Compose interpolation would
  otherwise treat password fragments such as `$TOKEN` as variables and alter the
  value before the container starts.
- Backend dashboard runtime uses `PUBLIC_API_BASE_URL`, `FRONTEND_BASE_URL`, optional `CORS_ALLOWED_ORIGINS`, `DISCORD_CLIENT_ID`, `DISCORD_CLIENT_SECRET`, `DISCORD_GUILD_ID`, and `DATABASE_*` in `backend_env`. For production, set `PUBLIC_API_BASE_URL` to `https://api.oficinamyuu.com.br`, `FRONTEND_BASE_URL` to `https://oficinamyuu.com.br`, and register `${PUBLIC_API_BASE_URL}/auth/discord/callback` in Discord.
- Bot and registrar runtime config use the shared MySQL-backed `config` table. The bots inventory passes MySQL `DATABASE_*` values to `bot` and `registrar` using a separate `oficina_bots` application user; create that schema/user manually before deploying the services.
- Add more bot containers as separate `oficina_services` entries only when they need separate processes, state directories, or images. Reuse the `bots_database_*` vars for any future bot container that should share the bots database user.
- CORS allows configured frontend origins and credentials. The backend exposes level card generation endpoints, `GET /health`, `/auth/*`, and `/birthdays`; it does not serve the dashboard UI.

For production browser access, keep `oficinamyuu.com.br` and `www.oficinamyuu.com.br` on Cloudflare Pages. Pages should build the frontend and serve `/dashboard` directly; do not proxy dashboard UI paths to the backend.

Validate before applying:

```sh
ansible-playbook -i ansible/inventories/prod/hosts.yml ansible/playbooks/site.yml --syntax-check
ansible-playbook -i ansible/inventories/prod/hosts.yml ansible/playbooks/site.yml --check --diff
```

On fresh hosts, check mode reports the apt prerequisite/repository work but
skips Docker package/service changes and Docker Compose deployment. Ansible does
not actually add the Docker apt repository or create runtime directories during
the dry run, so those steps are applied only by the real run.

Apply:

```sh
ansible-playbook -i ansible/inventories/prod/hosts.yml ansible/playbooks/site.yml
```

## Backend Validation

Backend unit tests do not require MySQL. Repository integration tests use a live temporary MySQL schema when `OFICINA_TEST_MYSQL_DSN` is set. Run them from `backend/cmd/`:

```sh
go test ./...
```
