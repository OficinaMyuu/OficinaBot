# Oficina Dashboard

React/Vite web app deployed by Cloudflare Pages. The current authenticated dashboard lives at `/dashboard`.

## Local development

By default, the dashboard frontend calls `http://localhost:8080`, so `npm run dev` expects a local API unless `VITE_API_BASE_URL` is set. Run the backend with dashboard OAuth pointed at Vite:

```powershell
$env:PUBLIC_API_BASE_URL='http://localhost:8080'
$env:FRONTEND_BASE_URL='http://localhost:5173'
$env:CORS_ALLOWED_ORIGINS='http://localhost:5173'
$env:DISCORD_CLIENT_ID='...'
$env:DISCORD_CLIENT_SECRET='...'
$env:DISCORD_GUILD_ID='...'
$env:DATABASE_HOST='localhost'
$env:DATABASE_PORT='3306'
$env:DATABASE_NAME='oficina_services'
$env:DATABASE_USER='oficina_backend'
$env:DATABASE_PASSWORD='...'
```

Then start Vite:

```powershell
npm install
npm run dev
```

Open `http://localhost:5173/dashboard`.

For frontend-only work against a non-local API, set `VITE_API_BASE_URL` before starting Vite:

```powershell
$env:VITE_API_BASE_URL='https://api.oficinamyuu.com.br'
npm run dev
```

That is useful for unauthenticated request debugging, but Discord OAuth is cookie and callback-origin sensitive. A production API configured with `FRONTEND_BASE_URL=https://oficinamyuu.com.br` will redirect successful logins back to production, not localhost. For authenticated local UI work without a local database, prefer running the backend locally against an approved remote/staging database, or use a staging API explicitly configured with `FRONTEND_BASE_URL=http://localhost:5173` and matching CORS.

Production builds are deployed by Cloudflare Pages, not by the Go backend. Configure the Pages project root as `frontend/` and use:

```text
Build command: npm ci && npm run build
Build output: dist
Environment: VITE_API_BASE_URL=https://api.oficinamyuu.com.br
```

If the Pages project root is the repository root instead, use `cd frontend && npm ci && npm run build` and `frontend/dist`.

The `public/_redirects` file keeps TanStack Router routes such as `/dashboard`, `/dashboard/birthdays`, and `/dashboard/tickets` working on direct browser refresh.
