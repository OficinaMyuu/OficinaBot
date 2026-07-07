# Oficina Dashboard

React dashboard served by the backend at `/dashboard`.

## Local development

Run the backend with dashboard OAuth pointed at Vite:

```powershell
$env:DASHBOARD_BASE_URL='http://localhost:5173/dashboard'
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

Production builds are copied into the backend Docker image and served from `/app/dashboard`. Public access uses `https://oficinamyuu.com.br/dashboard`, with Cloudflare Pages or a Cloudflare Worker proxying `/dashboard*` to the API origin.
