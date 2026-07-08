# Oficina Dashboard

React/Vite web app deployed by Cloudflare Pages. The current authenticated dashboard lives at `/dashboard`.

## Local development

Run the backend with dashboard OAuth pointed at Vite:

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

Production builds are deployed by Cloudflare Pages, not by the Go backend. Configure the Pages project root as `frontend/` and use:

```text
Build command: npm ci && npm run build
Build output: dist
Environment: VITE_API_BASE_URL=https://api.oficinamyuu.com.br
```

If the Pages project root is the repository root instead, use `cd frontend && npm ci && npm run build` and `frontend/dist`.

The `public/_redirects` file keeps TanStack Router routes such as `/dashboard` and `/dashboard/birthdays` working on direct browser refresh.
