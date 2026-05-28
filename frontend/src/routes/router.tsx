import { createRootRoute, createRoute, createRouter, Link, Outlet } from '@tanstack/react-router'
import Button from '../components/ui/Button'

// Root route layout
const rootRoute = createRootRoute({
  component: () => {
    return (
      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          minHeight: '100vh',
          backgroundColor: '#0f0c1b', // Dark purple-black background
          color: '#f3f0ff',
          fontFamily: 'system-ui, -apple-system, sans-serif',
        }}
      >
        {/* Header */}
        <header
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '16px 24px',
            backgroundColor: '#18122B', // Dark purple
            borderBottom: '1px solid #393053',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <span
              style={{
                fontSize: '20px',
                fontWeight: 'bold',
                background: 'linear-gradient(135deg, #a78bfa, #8b5cf6)',
                WebkitBackgroundClip: 'text',
                WebkitTextFillColor: 'transparent',
              }}
            >
              OficinaServices
            </span>
            <span
              style={{
                fontSize: '12px',
                backgroundColor: '#393053',
                padding: '2px 8px',
                borderRadius: '12px',
                color: '#c084fc',
                fontWeight: 'semibold',
              }}
            >
              Admin Dashboard
            </span>
          </div>

          <nav style={{ display: 'flex', gap: '20px' }}>
            <Link
              to="/"
              activeProps={{ style: { color: '#a78bfa', fontWeight: 'bold' } }}
              inactiveProps={{ style: { color: '#94a3b8' } }}
              style={{ textDecoration: 'none', transition: 'color 0.2s' }}
            >
              Dashboard
            </Link>
            <Link
              to="/logs"
              activeProps={{ style: { color: '#a78bfa', fontWeight: 'bold' } }}
              inactiveProps={{ style: { color: '#94a3b8' } }}
              style={{ textDecoration: 'none', transition: 'color 0.2s' }}
            >
              Logs
            </Link>
            <Link
              to="/punishments"
              activeProps={{ style: { color: '#a78bfa', fontWeight: 'bold' } }}
              inactiveProps={{ style: { color: '#94a3b8' } }}
              style={{ textDecoration: 'none', transition: 'color 0.2s' }}
            >
              Punishments
            </Link>
          </nav>
        </header>

        {/* Content */}
        <main
          style={{
            flex: 1,
            padding: '24px',
            maxWidth: '1200px',
            width: '100%',
            margin: '0 auto',
            boxSizing: 'border-box',
          }}
        >
          <Outlet />
        </main>
      </div>
    )
  },
})

// Index Route (Dashboard)
const indexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  component: () => {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
        <h1 style={{ margin: 0, fontSize: '28px', color: '#f3f0ff' }}>Overview</h1>
        <p style={{ color: '#94a3b8', margin: 0 }}>
          Welcome to the OficinaServices administrative dashboard. Here you can monitor bot logs,
          manage punishments, and configure server behavior.
        </p>

        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
            gap: '20px',
            marginTop: '10px',
          }}
        >
          {/* Card 1 */}
          <div
            style={{
              backgroundColor: '#18122B',
              borderRadius: '8px',
              padding: '20px',
              border: '1px solid #393053',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
              gap: '16px',
            }}
          >
            <div>
              <h3 style={{ margin: '0 0 10px 0', color: '#c084fc' }}>System Status</h3>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span
                  style={{
                    width: '8px',
                    height: '8px',
                    borderRadius: '50%',
                    backgroundColor: '#10b981',
                  }}
                ></span>
                <span style={{ fontSize: '14px' }}>All services operational</span>
              </div>
            </div>
            <div style={{ display: 'flex', gap: '8px' }}>
              <Button size="sm" onClick={() => alert('Performing system health check...')}>
                Run Health Check
              </Button>
              <Button variant="secondary" size="sm" onClick={() => alert('Refreshing status...')}>
                Refresh
              </Button>
            </div>
          </div>

          {/* Card 2 */}
          <div
            style={{
              backgroundColor: '#18122B',
              borderRadius: '8px',
              padding: '20px',
              border: '1px solid #393053',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
              gap: '16px',
            }}
          >
            <div>
              <h3 style={{ margin: '0 0 10px 0', color: '#c084fc' }}>Recent Sync</h3>
              <span style={{ fontSize: '14px', color: '#94a3b8' }}>
                OficinaBot active (2 mins ago)
              </span>
            </div>
            <div>
              <Button
                variant="danger"
                size="sm"
                onClick={() => alert('Restarting bot service sync connection...')}
              >
                Restart Sync
              </Button>
            </div>
          </div>
        </div>
      </div>
    )
  },
})

// Logs Route
const logsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/logs',
  component: () => {
    return (
      <div>
        <h1 style={{ margin: '0 0 20px 0', fontSize: '28px' }}>System Logs</h1>
        <p style={{ color: '#94a3b8' }}>View recent Discord message logs and moderation actions.</p>
        <div
          style={{
            backgroundColor: '#18122B',
            borderRadius: '8px',
            padding: '20px',
            border: '1px solid #393053',
            fontFamily: 'monospace',
            fontSize: '14px',
            color: '#cbd5e1',
          }}
        >
          <div>[01:00:23] INFO: Ingested event batch 1024</div>
          <div>[01:01:45] WARN: Discord gateway connection throttled</div>
          <div>[01:02:11] INFO: Successfully reconnected to shard #0</div>
        </div>
      </div>
    )
  },
})

// Punishments Route
const punishmentsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/punishments',
  component: () => {
    return (
      <div>
        <h1 style={{ margin: '0 0 20px 0', fontSize: '28px' }}>Punishments</h1>
        <p style={{ color: '#94a3b8' }}>Search, review, and issue punishments to guild members.</p>
        <div
          style={{
            backgroundColor: '#18122B',
            borderRadius: '8px',
            padding: '20px',
            border: '1px solid #393053',
            textAlign: 'center',
            color: '#94a3b8',
          }}
        >
          No punishments recorded in the current session.
        </div>
      </div>
    )
  },
})

// Create route tree
const routeTree = rootRoute.addChildren([indexRoute, logsRoute, punishmentsRoute])

// Create router
export const router = createRouter({ routeTree })

// Register the router instance for type safety
declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}
