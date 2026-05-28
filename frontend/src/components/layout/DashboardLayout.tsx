import React, { useState } from 'react'
import { Link } from '@tanstack/react-router'
import Button from '../ui/Button'
import { useAuth } from '../../context/AuthContext'

interface DashboardLayoutProps {
  children: React.ReactNode
  pageTitle: string
}

export const DashboardLayout: React.FC<DashboardLayoutProps> = ({ children, pageTitle }) => {
  const [isSidebarOpen, setIsSidebarOpen] = useState(true)
  const { user, logout } = useAuth()

  return (
    <div
      style={{
        display: 'flex',
        minHeight: '100vh',
        backgroundColor: 'var(--bg-deep)',
        color: 'var(--text-primary)',
        fontFamily: 'system-ui, -apple-system, sans-serif',
      }}
    >
      {/* Sidebar - Discord Channel List Aesthetic */}
      <aside
        style={{
          width: isSidebarOpen ? '260px' : '0px',
          opacity: isSidebarOpen ? 1 : 0,
          backgroundColor: 'var(--bg-sidebar)',
          borderRight: '1px solid var(--border-medium)',
          display: 'flex',
          flexDirection: 'column',
          transition: 'all 0.25s cubic-bezier(0.4, 0, 0.2, 1)',
          overflow: 'hidden',
          position: 'relative',
          zIndex: 10,
        }}
      >
        {/* Sidebar Header */}
        <div
          style={{
            padding: '16px 20px',
            borderBottom: '1px solid var(--border-medium)',
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            height: '56px',
            boxSizing: 'border-box',
          }}
        >
          <div
            style={{
              width: '24px',
              height: '24px',
              borderRadius: '6px',
              background: 'linear-gradient(135deg, var(--color-primary), var(--color-secondary))',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '12px',
              fontWeight: 'bold',
              color: '#fff',
            }}
          >
            O
          </div>
          <span
            style={{
              fontSize: '16px',
              fontWeight: 'bold',
              letterSpacing: '0.5px',
            }}
          >
            OficinaServices
          </span>
        </div>

        {/* Navigation Categories & Channels */}
        <div
          style={{
            padding: '20px 10px',
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
            gap: '20px',
          }}
        >
          <div>
            <div
              style={{
                fontSize: '11px',
                textTransform: 'uppercase',
                color: 'var(--text-muted)',
                fontWeight: 'bold',
                letterSpacing: '1px',
                padding: '0 10px 8px 10px',
              }}
            >
              Core Dashboard
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
              <Link
                to="/"
                activeProps={{
                  style: { backgroundColor: 'var(--bg-hover)', color: 'var(--text-primary)' },
                }}
                inactiveProps={{ style: { color: 'var(--text-secondary)' } }}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  padding: '8px 12px',
                  borderRadius: '6px',
                  textDecoration: 'none',
                  fontSize: '14px',
                  transition: 'all 0.2s',
                  fontWeight: '500',
                }}
              >
                <span style={{ color: 'var(--color-secondary)', opacity: 0.8 }}>#</span> overview
              </Link>
              <Link
                to="/logs"
                activeProps={{
                  style: { backgroundColor: 'var(--bg-hover)', color: 'var(--text-primary)' },
                }}
                inactiveProps={{ style: { color: 'var(--text-secondary)' } }}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  padding: '8px 12px',
                  borderRadius: '6px',
                  textDecoration: 'none',
                  fontSize: '14px',
                  transition: 'all 0.2s',
                  fontWeight: '500',
                }}
              >
                <span style={{ color: 'var(--color-secondary)', opacity: 0.8 }}>#</span> system-logs
              </Link>
              <Link
                to="/punishments"
                activeProps={{
                  style: { backgroundColor: 'var(--bg-hover)', color: 'var(--text-primary)' },
                }}
                inactiveProps={{ style: { color: 'var(--text-secondary)' } }}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  padding: '8px 12px',
                  borderRadius: '6px',
                  textDecoration: 'none',
                  fontSize: '14px',
                  transition: 'all 0.2s',
                  fontWeight: '500',
                }}
              >
                <span style={{ color: 'var(--color-secondary)', opacity: 0.8 }}>#</span> punishments
              </Link>
              <Link
                to="/registrations"
                activeProps={{
                  style: { backgroundColor: 'var(--bg-hover)', color: 'var(--text-primary)' },
                }}
                inactiveProps={{ style: { color: 'var(--text-secondary)' } }}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  padding: '8px 12px',
                  borderRadius: '6px',
                  textDecoration: 'none',
                  fontSize: '14px',
                  transition: 'all 0.2s',
                  fontWeight: '500',
                }}
              >
                <span style={{ color: 'var(--color-secondary)', opacity: 0.8 }}>#</span> member-registrations
              </Link>
              <Link
                to="/config"
                activeProps={{
                  style: { backgroundColor: 'var(--bg-hover)', color: 'var(--text-primary)' },
                }}
                inactiveProps={{ style: { color: 'var(--text-secondary)' } }}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  padding: '8px 12px',
                  borderRadius: '6px',
                  textDecoration: 'none',
                  fontSize: '14px',
                  transition: 'all 0.2s',
                  fontWeight: '500',
                }}
              >
                <span style={{ color: 'var(--color-secondary)', opacity: 0.8 }}>#</span> automod-config
              </Link>

              {/* Owner-only Admin Management Link */}
              {user?.isOwner && (
                <Link
                  to="/admin"
                  activeProps={{
                    style: { backgroundColor: 'var(--bg-hover)', color: 'var(--text-primary)' },
                  }}
                  inactiveProps={{ style: { color: 'var(--text-secondary)' } }}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '8px',
                    padding: '8px 12px',
                    borderRadius: '6px',
                    textDecoration: 'none',
                    fontSize: '14px',
                    transition: 'all 0.2s',
                    fontWeight: '500',
                  }}
                >
                  <span style={{ color: 'var(--color-secondary)', opacity: 0.8 }}>#</span>{' '}
                  admin-management
                </Link>
              )}
            </div>
          </div>
        </div>

        {/* Sidebar Footer User Details */}
        <div
          style={{
            padding: '12px 16px',
            backgroundColor: 'rgba(0, 0, 0, 0.2)',
            borderTop: '1px solid var(--border-medium)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            gap: '10px',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', overflow: 'hidden' }}>
            <div
              style={{
                width: '32px',
                height: '32px',
                borderRadius: '50%',
                backgroundColor: 'var(--bg-hover)',
                border: '2px solid var(--color-primary)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                fontWeight: 'bold',
                color: 'var(--color-secondary)',
                fontSize: '13px',
                flexShrink: 0,
              }}
            >
              {user?.username?.[0] || 'A'}
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
              <span
                style={{
                  fontSize: '13px',
                  fontWeight: 'bold',
                  textOverflow: 'ellipsis',
                  overflow: 'hidden',
                  whiteSpace: 'nowrap',
                }}
              >
                {user?.username || 'Admin#0001'}
              </span>
              <span
                style={{
                  fontSize: '11px',
                  color: 'var(--text-muted)',
                  textOverflow: 'ellipsis',
                  overflow: 'hidden',
                  whiteSpace: 'nowrap',
                }}
              >
                {user?.isOwner ? 'Server Owner' : 'Guild Admin'}
              </span>
            </div>
          </div>
          <Button
            variant="ghost"
            size="sm"
            style={{ padding: '4px', minWidth: 'auto', color: 'var(--text-muted)' }}
            onClick={logout}
            title="Log out"
          >
            ❌
          </Button>
        </div>
      </aside>

      {/* Main Workspace Frame */}
      <div
        style={{
          flex: 1,
          display: 'flex',
          flexDirection: 'column',
          minWidth: 0,
        }}
      >
        {/* Top Context Bar */}
        <header
          style={{
            height: '56px',
            backgroundColor: 'var(--bg-panel)',
            borderBottom: '1px solid var(--border-medium)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '0 24px',
            boxSizing: 'border-box',
          }}
        >
          <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
            {/* Sidebar toggle button */}
            <button
              onClick={() => setIsSidebarOpen(!isSidebarOpen)}
              style={{
                background: 'none',
                border: 'none',
                color: 'var(--text-secondary)',
                cursor: 'pointer',
                fontSize: '18px',
                padding: '4px',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                outline: 'none',
              }}
              title={isSidebarOpen ? 'Collapse Sidebar' : 'Expand Sidebar'}
            >
              ☰
            </button>
            <h2
              style={{
                margin: 0,
                fontSize: '16px',
                fontWeight: 'bold',
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
              }}
            >
              <span style={{ color: 'var(--text-secondary)' }}>#</span>
              {pageTitle}
            </h2>
          </div>

          {/* Sync Status Badge */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
            <div
              style={{
                display: 'flex',
                alignItems: 'center',
                gap: '6px',
                backgroundColor: 'rgba(16, 185, 129, 0.1)',
                border: '1px solid rgba(16, 185, 129, 0.2)',
                padding: '4px 10px',
                borderRadius: '12px',
                fontSize: '12px',
                color: 'var(--color-success)',
                fontWeight: '600',
              }}
            >
              <span
                style={{
                  width: '6px',
                  height: '6px',
                  borderRadius: '50%',
                  backgroundColor: 'var(--color-success)',
                  display: 'inline-block',
                  boxShadow: '0 0 8px var(--color-success)',
                }}
              ></span>
              OficinaBot: Synced
            </div>
          </div>
        </header>

        {/* Dashboard Dynamic Page Content */}
        <main
          style={{
            flex: 1,
            padding: '24px',
            overflowY: 'auto',
            boxSizing: 'border-box',
          }}
          className="animate-fade-in"
        >
          {children}
        </main>
      </div>
    </div>
  )
}

export default DashboardLayout
