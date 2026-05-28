/* eslint-disable react-refresh/only-export-components */
import { createRootRoute, createRoute, createRouter, Link, Outlet } from '@tanstack/react-router'
import React, { useState } from 'react'
import DashboardLayout from '../components/layout/DashboardLayout'
import Button from '../components/ui/Button'
import { Table, Thead, Tbody, Th, Td, Tr } from '../components/ui/Table'
import { Filters, SearchInput, Select } from '../components/ui/Filters'
import { FormField, Input, Switch, Textarea } from '../components/ui/Form'
import Modal from '../components/ui/Modal'
import { useToast } from '../components/ui/Toast'
import { LoadingState, EmptyState, ErrorState } from '../components/ui/FeedbackStates'
import AuthGuard from '../components/auth/AuthGuard'
import AdminManagement from '../components/admin/AdminManagement'
import Login from '../components/auth/Login'

// Root route (outlet wrapper)
const rootRoute = createRootRoute({
  component: () => <Outlet />
})

// ==========================================
// 1. Overview Component & Route
// ==========================================
export const OverviewComponent: React.FC = () => {
  const { showToast } = useToast()
  const [isDiagnosticOpen, setIsDiagnosticOpen] = useState(false)
  const [isDiagnosticsRunning, setIsDiagnosticsRunning] = useState(false)
  const [maintenanceMode, setMaintenanceMode] = useState(false)
  const [maxReminders, setMaxReminders] = useState('5')
  const [customWelcome, setCustomWelcome] = useState('Bem-vindo à Oficina!')

  const handleSaveSettings = () => {
    showToast('Dashboard configurations saved successfully!', 'success')
  }

  const runDiagnostics = () => {
    setIsDiagnosticsRunning(true)
    showToast('Initiating system diagnostics...', 'info')
    setTimeout(() => {
      setIsDiagnosticsRunning(false)
      setIsDiagnosticOpen(false)
      showToast('All diagnostics passed! System integrity 100%.', 'success')
    }, 2500)
  }

  return (
    <DashboardLayout pageTitle="Overview">
      <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
        
        {/* Metric Cards Row */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
          gap: '20px'
        }}>
          {/* Card 1: System Health */}
          <div style={{
            backgroundColor: 'var(--bg-panel)',
            borderRadius: '8px',
            padding: '20px',
            border: '1px solid var(--border-medium)',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            gap: '16px'
          }}>
            <div>
              <h3 style={{ margin: '0 0 12px 0', color: 'var(--color-secondary)', fontSize: '13px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                System Health
              </h3>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
                <span style={{
                  width: '8px',
                  height: '8px',
                  borderRadius: '50%',
                  backgroundColor: 'var(--color-success)',
                  boxShadow: '0 0 8px var(--color-success)'
                }} />
                <span style={{ fontSize: '15px', fontWeight: 'bold' }}>All systems operational</span>
              </div>
              <span style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                Latency: 12ms | CPU: 1.4% | RAM: 420MB / 1GB
              </span>
            </div>
            <div style={{ display: 'flex', gap: '8px' }}>
              <Button size="sm" onClick={() => setIsDiagnosticOpen(true)}>Run Diagnostics</Button>
              <Button variant="secondary" size="sm" onClick={() => showToast('Cache flushed successfully.', 'info')}>
                Flush Cache
              </Button>
            </div>
          </div>

          {/* Card 2: Sync Status */}
          <div style={{
            backgroundColor: 'var(--bg-panel)',
            borderRadius: '8px',
            padding: '20px',
            border: '1px solid var(--border-medium)',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            gap: '16px'
          }}>
            <div>
              <h3 style={{ margin: '0 0 12px 0', color: 'var(--color-secondary)', fontSize: '13px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                Bot Gateway Connection
              </h3>
              <div style={{ fontSize: '15px', fontWeight: 'bold', display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
                <span>🤖</span>
                <span>OficinaBot (Main Guild)</span>
              </div>
              <span style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                Shard #0 running on Hostsquare. Last heartbeat sync: 44s ago.
              </span>
            </div>
            <div>
              <Button variant="danger" size="sm" onClick={() => showToast('Reconnection requested to Shard #0', 'warning')}>
                Reconnect Shard
              </Button>
            </div>
          </div>

          {/* Card 3: Database Vitals */}
          <div style={{
            backgroundColor: 'var(--bg-panel)',
            borderRadius: '8px',
            padding: '20px',
            border: '1px solid var(--border-medium)',
            display: 'flex',
            flexDirection: 'column',
            justifyContent: 'space-between',
            gap: '16px'
          }}>
            <div>
              <h3 style={{ margin: '0 0 12px 0', color: 'var(--color-secondary)', fontSize: '13px', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
                Database Storage
              </h3>
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
                <span style={{
                  width: '8px',
                  height: '8px',
                  borderRadius: '50%',
                  backgroundColor: 'var(--color-success)',
                  boxShadow: '0 0 8px var(--color-success)'
                }} />
                <span style={{ fontSize: '15px', fontWeight: 'bold' }}>SQLite WAL Active</span>
              </div>
              <span style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>
                Disk Usage: 1.4GB | Pooled connections: 1 (single-lock)
              </span>
            </div>
            <div>
              <Button variant="secondary" size="sm" onClick={() => showToast('Database integrity optimized.', 'success')}>
                Vacuum DB
              </Button>
            </div>
          </div>
        </div>

        {/* Dynamic Split panels: Quick Config & Recent Automod Logs */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))',
          gap: '24px'
        }}>
          {/* Quick Settings Configuration */}
          <div style={{
            backgroundColor: 'var(--bg-panel)',
            border: '1px solid var(--border-medium)',
            borderRadius: '8px',
            padding: '24px'
          }}>
            <h3 style={{ margin: '0 0 20px 0', fontSize: '16px', fontWeight: 'bold', color: 'var(--text-primary)' }}>
              Quick Settings Configuration
            </h3>
            
            <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <FormField
                label="Custom Welcome Banner Text"
                helperText="Displayed inside the welcome card when new users join."
              >
                <Input
                  value={customWelcome}
                  onChange={(e) => setCustomWelcome(e.target.value)}
                  placeholder="Enter welcome message..."
                />
              </FormField>

              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
                <FormField
                  label="Max Reminders Limit"
                  helperText="Concurrent reminder cap."
                >
                  <Input
                    type="number"
                    value={maxReminders}
                    onChange={(e) => setMaxReminders(e.target.value)}
                  />
                </FormField>

                <FormField
                  label="Maintenance Mode"
                  helperText="Block non-admin actions."
                  style={{ justifyContent: 'center' }}
                >
                  <div style={{ paddingTop: '8px' }}>
                    <Switch
                      checked={maintenanceMode}
                      onChange={(val) => {
                        setMaintenanceMode(val)
                        showToast(`Maintenance mode is now ${val ? 'ENABLED' : 'DISABLED'}`, val ? 'warning' : 'info')
                      }}
                      labelAfter={maintenanceMode ? 'Locked' : 'Open'}
                    />
                  </div>
                </FormField>
              </div>

              <div style={{ marginTop: '10px' }}>
                <Button onClick={handleSaveSettings}>Save Configuration</Button>
              </div>
            </div>
          </div>

          {/* Recent Automod Flags */}
          <div style={{
            backgroundColor: 'var(--bg-panel)',
            border: '1px solid var(--border-medium)',
            borderRadius: '8px',
            padding: '24px',
            display: 'flex',
            flexDirection: 'column',
            gap: '16px'
          }}>
            <h3 style={{ margin: 0, fontSize: '16px', fontWeight: 'bold', color: 'var(--text-primary)' }}>
              Recent Automod Filters Activity
            </h3>
            
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {/* Event 1 */}
              <div style={{
                padding: '12px',
                backgroundColor: 'rgba(0,0,0,0.15)',
                border: '1px solid var(--border-light)',
                borderRadius: '6px',
                fontSize: '13px'
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                  <span style={{ color: 'var(--color-warning)', fontWeight: 'bold' }}>⚠️ Link Blocked</span>
                  <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>10 mins ago</span>
                </div>
                <div style={{ color: 'var(--text-primary)' }}>
                  User <strong>Spike#9999</strong> sent blocked link format in #geral.
                </div>
              </div>

              {/* Event 2 */}
              <div style={{
                padding: '12px',
                backgroundColor: 'rgba(0,0,0,0.15)',
                border: '1px solid var(--border-light)',
                borderRadius: '6px',
                fontSize: '13px'
              }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '4px' }}>
                  <span style={{ color: 'var(--color-danger)', fontWeight: 'bold' }}>🛑 Auto-Timeout</span>
                  <span style={{ fontSize: '11px', color: 'var(--text-muted)' }}>45 mins ago</span>
                </div>
                <div style={{ color: 'var(--text-primary)' }}>
                  User <strong>Spammer#1212</strong> timed out for 10m: Spam messages threshold matched.
                </div>
              </div>
            </div>
            
            <div style={{ textAlign: 'right' }}>
              <Link to="/logs" style={{ fontSize: '13px', color: 'var(--color-secondary)', textDecoration: 'none' }}>
                View system-logs →
              </Link>
            </div>
          </div>
        </div>
      </div>

      {/* Diagnostic Modal */}
      <Modal
        isOpen={isDiagnosticOpen}
        onClose={() => !isDiagnosticsRunning && setIsDiagnosticOpen(false)}
        title="Diagnostic Suite"
        footer={
          <>
            <Button variant="secondary" disabled={isDiagnosticsRunning} onClick={() => setIsDiagnosticOpen(false)}>
              Close
            </Button>
            <Button isLoading={isDiagnosticsRunning} onClick={runDiagnostics}>
              Execute Integrity Checks
            </Button>
          </>
        }
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <p style={{ margin: 0 }}>This suite executes real-time validation checks against the bot gateway, SQLite read/write thresholds, and active memory allocation loops.</p>
          <div style={{
            backgroundColor: 'rgba(0,0,0,0.2)',
            border: '1px solid var(--border-medium)',
            borderRadius: '6px',
            padding: '12px',
            fontFamily: 'monospace',
            fontSize: '12px'
          }}>
            <div>- Shard validation ping (Gateway)</div>
            <div>- SQLite raw write checks (Config pool)</div>
            <div>- File system permission locks</div>
            <div>- Task scheduler loop frequency</div>
          </div>
        </div>
      </Modal>
    </DashboardLayout>
  )
}

const indexRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/',
  component: OverviewComponent
})

// ==========================================
// 2. Logs Component & Route (Paginated)
// ==========================================
const mockLogs = [
  { id: '1', level: 'info', author: 'Leonardo#0001', channel: 'geral', message: 'User updated custom status', time: '01:10:22' },
  { id: '2', level: 'info', author: 'OficinaBot', channel: 'logs', message: 'Synced bad words configuration', time: '01:12:45' },
  { id: '3', level: 'warning', author: 'RandomGamer#2341', channel: 'geral', message: 'Automod flagged message: spam link pattern matched', time: '01:14:12' },
  { id: '4', level: 'error', author: 'SystemError', channel: 'core', message: 'Discord REST request failed: rate limit hit on guild member list', time: '01:15:33' },
  { id: '5', level: 'info', author: 'ModGuy#1232', channel: 'logs', message: 'Allowlisted co-admin user accounts', time: '01:18:22' },
  { id: '6', level: 'info', author: 'OficinaBot', channel: 'system', message: 'Successfully scheduled Quartz cleanup backup task', time: '01:20:00' },
  { id: '7', level: 'warning', author: 'NaughtyUser#8888', channel: 'geral', message: 'Flagged word used: blocked slurs matched', time: '01:21:40' },
  { id: '8', level: 'error', author: 'OficinaBot', channel: 'persistence', message: 'Failed to write WAL config commit logs (locked pool)', time: '01:23:12' }
]

export const LogsComponent: React.FC = () => {
  const { showToast } = useToast()
  const [search, setSearch] = useState('')
  const [levelFilter, setLevelFilter] = useState('all')
  const [isLoading, setIsLoading] = useState(false)
  
  // Pagination states
  const [currentPage, setCurrentPage] = useState(1)
  const [pageSize, setPageSize] = useState(3)

  const handleClearSearch = () => {
    setSearch('')
    showToast('Cleared search filters.', 'info')
  }

  const filteredLogs = mockLogs.filter(log => {
    const matchesSearch = log.message.toLowerCase().includes(search.toLowerCase()) || 
                          log.author.toLowerCase().includes(search.toLowerCase())
    const matchesLevel = levelFilter === 'all' || log.level === levelFilter
    return matchesSearch && matchesLevel
  })

  // Calculate slice pages
  const totalPages = Math.ceil(filteredLogs.length / pageSize) || 1
  const paginatedLogs = filteredLogs.slice((currentPage - 1) * pageSize, currentPage * pageSize)

  const getLevelStyle = (level: string): React.CSSProperties => {
    switch (level) {
      case 'success': return { color: 'var(--color-success)', backgroundColor: 'rgba(16, 185, 129, 0.15)', border: '1px solid rgba(16, 185, 129, 0.3)' }
      case 'error': return { color: 'var(--color-danger)', backgroundColor: 'rgba(239, 68, 68, 0.15)', border: '1px solid rgba(239, 68, 68, 0.3)' }
      case 'warning': return { color: 'var(--color-warning)', backgroundColor: 'rgba(245, 158, 11, 0.15)', border: '1px solid rgba(245, 158, 11, 0.3)' }
      case 'info':
      default: return { color: 'var(--color-info)', backgroundColor: 'rgba(59, 130, 246, 0.15)', border: '1px solid rgba(59, 130, 246, 0.3)' }
    }
  }

  const simulateLoading = () => {
    setIsLoading(true)
    setTimeout(() => {
      setIsLoading(false)
      showToast('Logs log-pool refreshed!', 'success')
    }, 1200)
  }

  const changePage = (page: number) => {
    setCurrentPage(page)
    showToast(`Navigated to page ${page}`, 'info')
  }

  return (
    <DashboardLayout pageTitle="System Logs">
      <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
        
        {/* Filters Bar */}
        <Filters>
          <SearchInput
            value={search}
            onChange={(e) => {
              setSearch(e.target.value)
              setCurrentPage(1)
            }}
            placeholder="Search logs by message or username..."
            onClear={handleClearSearch}
          />
          
          <Select
            label="Level"
            value={levelFilter}
            onChange={(e) => {
              setLevelFilter(e.target.value)
              setCurrentPage(1)
            }}
          >
            <option value="all">All Levels</option>
            <option value="info">Info</option>
            <option value="warning">Warning</option>
            <option value="error">Error</option>
          </Select>

          <Select
            label="Logs Per Page"
            value={pageSize.toString()}
            onChange={(e) => {
              setPageSize(parseInt(e.target.value))
              setCurrentPage(1)
            }}
          >
            <option value="3">3 items</option>
            <option value="5">5 items</option>
            <option value="10">10 items</option>
          </Select>

          <Button variant="secondary" onClick={simulateLoading}>
            Refresh Logs
          </Button>
        </Filters>

        {/* Feedback states */}
        {isLoading ? (
          <LoadingState />
        ) : paginatedLogs.length === 0 ? (
          <EmptyState
            title="No logs found"
            description="No logging records matched your active filter values. Try clearing text or selecting all levels."
            icon="🔎"
            actionLabel="Reset Search"
            onAction={handleClearSearch}
          />
        ) : (
          /* Table Data */
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <Table>
              <Thead>
                <Tr>
                  <Th style={{ width: '80px' }}>Time</Th>
                  <Th style={{ width: '100px' }}>Level</Th>
                  <Th style={{ width: '150px' }}>Author</Th>
                  <Th style={{ width: '100px' }}>Channel</Th>
                  <Th>Message</Th>
                </Tr>
              </Thead>
              <Tbody>
                {paginatedLogs.map(log => (
                  <Tr key={log.id}>
                    <Td style={{ fontFamily: 'monospace', color: 'var(--text-muted)' }}>{log.time}</Td>
                    <Td>
                      <span style={{
                        padding: '2px 8px',
                        borderRadius: '10px',
                        fontSize: '11px',
                        fontWeight: 'bold',
                        textTransform: 'uppercase',
                        ...getLevelStyle(log.level)
                      }}>
                        {log.level}
                      </span>
                    </Td>
                    <Td style={{ fontWeight: 'bold' }}>{log.author}</Td>
                    <Td style={{ color: 'var(--color-secondary)' }}>#{log.channel}</Td>
                    <Td style={{ color: 'var(--text-primary)' }}>{log.message}</Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>

            {/* Pagination Controls */}
            <div style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '12px 16px',
              backgroundColor: 'var(--bg-panel)',
              borderRadius: '8px',
              border: '1px solid var(--border-medium)',
              fontSize: '13px',
              color: 'var(--text-secondary)'
            }}>
              <div>
                Showing {(currentPage - 1) * pageSize + 1} to {Math.min(currentPage * pageSize, filteredLogs.length)} of {filteredLogs.length} logs
              </div>
              
              <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={currentPage === 1}
                  onClick={() => changePage(1)}
                  style={{ padding: '6px 8px', minWidth: 'auto' }}
                >
                  «
                </Button>
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={currentPage === 1}
                  onClick={() => changePage(currentPage - 1)}
                  style={{ padding: '6px 8px', minWidth: 'auto' }}
                >
                  ‹
                </Button>
                
                <span style={{ margin: '0 8px', fontWeight: 'semibold' }}>
                  Page {currentPage} of {totalPages}
                </span>

                <Button
                  variant="secondary"
                  size="sm"
                  disabled={currentPage === totalPages}
                  onClick={() => changePage(currentPage + 1)}
                  style={{ padding: '6px 8px', minWidth: 'auto' }}
                >
                  ›
                </Button>
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={currentPage === totalPages}
                  onClick={() => changePage(totalPages)}
                  style={{ padding: '6px 8px', minWidth: 'auto' }}
                >
                  »
                </Button>
              </div>
            </div>
          </div>
        )}

      </div>
    </DashboardLayout>
  )
}

const logsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/logs',
  component: LogsComponent
})

// ==========================================
// 3. Punishments Component & Route (Detail Modal)
// ==========================================
interface PunishmentRecord {
  id: string
  user: string
  type: string
  reason: string
  date: string
  moderator: string
  duration?: string
  status: 'ACTIVE' | 'REVOKED' | 'EXPIRED'
}

const initialPunishments: PunishmentRecord[] = [
  { id: 'pun-432a', user: 'BadUser#1111', type: 'BAN', reason: 'Toxicity & slurs in geral channel', date: 'May 25, 2026', moderator: 'Leonardo#0001', status: 'ACTIVE' },
  { id: 'pun-903b', user: 'SpammyBot#4321', type: 'KICK', reason: 'Automated token invite links spamming', date: 'May 27, 2026', moderator: 'OficinaBot', status: 'EXPIRED' },
  { id: 'pun-882d', user: 'NaughtyUser#8888', type: 'TIMEOUT', reason: 'Repeated caps invite link spam', date: 'May 28, 2026', moderator: 'Leonardo#0001', duration: '30 minutes', status: 'ACTIVE' }
]

export const PunishmentsComponent: React.FC = () => {
  const { showToast } = useToast()
  const [punishments, setPunishments] = useState<PunishmentRecord[]>(initialPunishments)
  const [user, setUser] = useState('')
  const [type, setType] = useState('WARN')
  const [reason, setReason] = useState('')
  const [formError, setFormError] = useState<{ user?: string; reason?: string }>({})
  
  // Interactive detail Modal states
  const [activeInspector, setActiveInspector] = useState<PunishmentRecord | null>(null)

  // Simulate error state showcase
  const [isLoadFailed, setIsLoadFailed] = useState(false)

  const handleIssuePunishment = (e: React.FormEvent) => {
    e.preventDefault()
    
    const errors: { user?: string; reason?: string } = {}
    if (!user) errors.user = 'Discord username or user ID is required.'
    if (!reason) errors.reason = 'A valid punishment reason must be documented.'

    if (Object.keys(errors).length > 0) {
      setFormError(errors)
      showToast('Failed to issue punishment. Please verify all inputs.', 'error')
      return
    }

    setFormError({})
    const newRecord: PunishmentRecord = {
      id: `pun-${Math.random().toString(36).substring(2, 6)}`,
      user,
      type,
      reason,
      date: 'Today',
      moderator: 'Leonardo#0001',
      status: 'ACTIVE'
    }

    setPunishments([newRecord, ...punishments])
    setUser('')
    setReason('')
    showToast(`Punishment issued successfully: ${type} against ${user}`, 'success')
  }

  const handleRevokePunishment = (id: string) => {
    setPunishments(prev => prev.map(p => {
      if (p.id === id) {
        return { ...p, status: 'REVOKED' }
      }
      return p
    }))
    setActiveInspector(null)
    showToast('Punishment has been successfully revoked.', 'warning')
  }

  if (isLoadFailed) {
    return (
      <DashboardLayout pageTitle="Punishments">
        <ErrorState
          title="Database Connection Failure"
          message="Unable to read the sqlite configurations or establish bearered client syncs."
          onRetry={() => {
            setIsLoadFailed(false)
            showToast('Database connections restored successfully.', 'success')
          }}
        />
      </DashboardLayout>
    )
  }

  return (
    <DashboardLayout pageTitle="Punishments">
      <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
        
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ fontSize: '14px', color: 'var(--text-secondary)' }}>
            Manage member behaviors, issue bans, kicks, or time-out warn penalties.
          </span>
          <Button variant="secondary" size="sm" onClick={() => setIsLoadFailed(true)}>
            Simulate Failure State
          </Button>
        </div>

        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
          gap: '24px',
          alignItems: 'start'
        }}>
          {/* Action Form */}
          <form onSubmit={handleIssuePunishment} style={{
            backgroundColor: 'var(--bg-panel)',
            border: '1px solid var(--border-medium)',
            borderRadius: '8px',
            padding: '20px',
            display: 'flex',
            flexDirection: 'column',
            gap: '16px'
          }}>
            <h3 style={{ margin: 0, color: 'var(--text-primary)', fontSize: '16px', fontWeight: 'bold' }}>
              Issue Punishment
            </h3>

            <FormField label="Discord Member User" error={formError.user}>
              <Input
                value={user}
                onChange={(e) => setUser(e.target.value)}
                placeholder="e.g. Leonardo#0001 or ID"
                hasError={!!formError.user}
              />
            </FormField>

            <FormField label="Punishment Action Type">
              <select
                value={type}
                onChange={(e) => setType(e.target.value)}
                style={{
                  width: '100%',
                  padding: '10px 14px',
                  backgroundColor: 'var(--bg-input)',
                  border: '1px solid var(--border-medium)',
                  borderRadius: '6px',
                  color: 'var(--text-primary)',
                  fontSize: '14px',
                  outline: 'none',
                  cursor: 'pointer'
                }}
              >
                <option value="WARN">WARN (Formal penalty)</option>
                <option value="TIMEOUT">TIMEOUT (Mute connection)</option>
                <option value="KICK">KICK (Evict from guild)</option>
                <option value="BAN">BAN (Permanent blacklist)</option>
              </select>
            </FormField>

            <FormField label="Reason Description" error={formError.reason}>
              <Textarea
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="Describe violation rules matched..."
                hasError={!!formError.reason}
              />
            </FormField>

            <Button type="submit" variant="danger">
              Apply Action
            </Button>
          </form>

          {/* List Table panel */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <h3 style={{ margin: 0, color: 'var(--text-primary)', fontSize: '16px', fontWeight: 'bold' }}>
              Active Punishment History
            </h3>

            {punishments.length === 0 ? (
              <EmptyState
                title="Zero active penalties"
                description="Guild moderation is perfectly quiet. No timeouts or bans recorded in the database."
                icon="🕊️"
              />
            ) : (
              <Table>
                <Thead>
                  <Tr>
                    <Th>User</Th>
                    <Th style={{ width: '100px' }}>Type</Th>
                    <Th style={{ width: '100px' }}>Status</Th>
                    <Th style={{ width: '80px', textAlign: 'center' }}>Details</Th>
                  </Tr>
                </Thead>
                <Tbody>
                  {punishments.map((p) => (
                    <Tr key={p.id}>
                      <Td style={{ fontWeight: 'bold' }}>{p.user}</Td>
                      <Td>
                        <span style={{
                          padding: '2px 6px',
                          borderRadius: '4px',
                          fontSize: '11px',
                          fontWeight: 'bold',
                          backgroundColor: p.type === 'BAN' ? 'rgba(239, 68, 68, 0.15)' : 'rgba(245, 158, 11, 0.15)',
                          color: p.type === 'BAN' ? 'var(--color-danger)' : 'var(--color-warning)',
                          border: `1px solid ${p.type === 'BAN' ? 'rgba(239, 68, 68, 0.2)' : 'rgba(245, 158, 11, 0.2)'}`
                        }}>
                          {p.type}
                        </span>
                      </Td>
                      <Td>
                        <span style={{
                          fontSize: '11px',
                          fontWeight: 'bold',
                          color: p.status === 'ACTIVE' ? 'var(--color-success)' : 'var(--text-muted)'
                        }}>
                          ● {p.status}
                        </span>
                      </Td>
                      <Td style={{ textAlign: 'center' }}>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setActiveInspector(p)}
                          style={{ padding: '2px 6px', fontSize: '12px', color: 'var(--color-secondary)' }}
                        >
                          Inspect
                        </Button>
                      </Td>
                    </Tr>
                  ))}
                </Tbody>
              </Table>
            )}
          </div>
        </div>

      </div>

      {/* Punishment Detail Inspector Modal */}
      <Modal
        isOpen={activeInspector !== null}
        onClose={() => setActiveInspector(null)}
        title="Punishment Inspector"
        footer={
          <>
            <Button variant="secondary" onClick={() => setActiveInspector(null)}>
              Close Detail
            </Button>
            {activeInspector && activeInspector.status === 'ACTIVE' && (
              <Button variant="danger" onClick={() => handleRevokePunishment(activeInspector.id)}>
                Revoke Punishment
              </Button>
            )}
          </>
        }
      >
        {activeInspector && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{
              display: 'grid',
              gridTemplateColumns: '1fr 1fr',
              gap: '12px',
              backgroundColor: 'rgba(0,0,0,0.15)',
              padding: '16px',
              borderRadius: '6px',
              border: '1px solid var(--border-medium)',
              fontSize: '13px'
            }}>
              <div>
                <span style={{ color: 'var(--text-muted)' }}>Case UUID:</span>
                <div style={{ fontFamily: 'monospace', fontWeight: 'bold' }}>{activeInspector.id}</div>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)' }}>Date Issued:</span>
                <div style={{ fontWeight: 'bold' }}>{activeInspector.date}</div>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)' }}>Target Account:</span>
                <div style={{ fontWeight: 'bold', color: 'var(--text-primary)' }}>{activeInspector.user}</div>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)' }}>Responsible Moderator:</span>
                <div style={{ fontWeight: 'bold' }}>{activeInspector.moderator}</div>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)' }}>Punishment Mode:</span>
                <div>
                  <span style={{
                    padding: '1px 6px',
                    borderRadius: '4px',
                    fontSize: '10px',
                    fontWeight: 'bold',
                    backgroundColor: activeInspector.type === 'BAN' ? 'rgba(239, 68, 68, 0.15)' : 'rgba(245, 158, 11, 0.15)',
                    color: activeInspector.type === 'BAN' ? 'var(--color-danger)' : 'var(--color-warning)'
                  }}>
                    {activeInspector.type}
                  </span>
                </div>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)' }}>Status State:</span>
                <div style={{ fontWeight: 'bold' }}>{activeInspector.status}</div>
              </div>
            </div>

            <div>
              <span style={{ fontSize: '12px', fontWeight: 'bold', textTransform: 'uppercase', color: 'var(--text-muted)' }}>
                Documented Offense Reason
              </span>
              <div style={{
                padding: '12px',
                backgroundColor: 'rgba(0,0,0,0.1)',
                border: '1px solid var(--border-light)',
                borderRadius: '6px',
                marginTop: '6px',
                fontSize: '13px',
                color: 'var(--text-primary)',
                lineHeight: '1.5'
              }}>
                {activeInspector.reason}
              </div>
            </div>
          </div>
        )}
      </Modal>
    </DashboardLayout>
  )
}

const punishmentsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/punishments',
  component: () => (
    <AuthGuard>
      <PunishmentsComponent />
    </AuthGuard>
  )
})

// ==========================================
// 4. Registrations Component & Route (New!)
// ==========================================
interface MemberRegistration {
  id: string
  username: string
  discordId: string
  joinDate: string
  status: 'APPROVED' | 'REJECTED' | 'PENDING'
}

const initialRegistrations: MemberRegistration[] = [
  { id: 'reg-01', username: 'MemberSpike#2211', discordId: '221144339900112233', joinDate: 'May 26, 2026', status: 'PENDING' },
  { id: 'reg-02', username: 'CozyGamer#3000', discordId: '300088776655443322', joinDate: 'May 27, 2026', status: 'APPROVED' },
  { id: 'reg-03', username: 'SpammerBot#0011', discordId: '110099887766554433', joinDate: 'May 28, 2026', status: 'REJECTED' }
]

export const RegistrationsComponent: React.FC = () => {
  const { showToast } = useToast()
  const [regs, setRegs] = useState<MemberRegistration[]>(initialRegistrations)

  const handleUpdateStatus = (id: string, newStatus: 'APPROVED' | 'REJECTED') => {
    setRegs(prev => prev.map(r => {
      if (r.id === id) {
        return { ...r, status: newStatus }
      }
      return r
    }))
    
    showToast(`Registration request is now ${newStatus}!`, newStatus === 'APPROVED' ? 'success' : 'error')
  }

  const getStatusBadge = (status: string): React.CSSProperties => {
    switch (status) {
      case 'APPROVED': return { color: 'var(--color-success)', backgroundColor: 'rgba(16, 185, 129, 0.15)', border: '1px solid rgba(16, 185, 129, 0.2)' }
      case 'REJECTED': return { color: 'var(--color-danger)', backgroundColor: 'rgba(239, 68, 68, 0.15)', border: '1px solid rgba(239, 68, 68, 0.2)' }
      case 'PENDING':
      default: return { color: 'var(--color-warning)', backgroundColor: 'rgba(245, 158, 11, 0.15)', border: '1px solid rgba(245, 158, 11, 0.2)' }
    }
  }

  return (
    <DashboardLayout pageTitle="Member Registrations">
      <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
        <p style={{ margin: 0, fontSize: '14px', color: 'var(--text-secondary)' }}>
          Review recently submitted member applications and verify registration status.
        </p>

        <Table>
          <Thead>
            <Tr>
              <Th>Username</Th>
              <Th>Discord User ID</Th>
              <Th>Join Date</Th>
              <Th>Verification Status</Th>
              <Th style={{ width: '180px', textAlign: 'center' }}>Moderate Request</Th>
            </Tr>
          </Thead>
          <Tbody>
            {regs.map((reg) => (
              <Tr key={reg.id}>
                <Td style={{ fontWeight: 'bold' }}>{reg.username}</Td>
                <Td style={{ fontFamily: 'monospace', color: 'var(--text-muted)' }}>{reg.discordId}</Td>
                <Td>{reg.joinDate}</Td>
                <Td>
                  <span style={{
                    padding: '2px 8px',
                    borderRadius: '4px',
                    fontSize: '11px',
                    fontWeight: 'bold',
                    ...getStatusBadge(reg.status)
                  }}>
                    {reg.status}
                  </span>
                </Td>
                <Td style={{ display: 'flex', justifyContent: 'center', gap: '8px' }}>
                  {reg.status === 'PENDING' ? (
                    <>
                      <Button
                        size="sm"
                        onClick={() => handleUpdateStatus(reg.id, 'APPROVED')}
                      >
                        Approve
                      </Button>
                      <Button
                        variant="secondary"
                        size="sm"
                        onClick={() => handleUpdateStatus(reg.id, 'REJECTED')}
                        style={{ color: 'var(--color-danger)' }}
                      >
                        Reject
                      </Button>
                    </>
                  ) : (
                    <span style={{ fontSize: '12px', color: 'var(--text-muted)', fontStyle: 'italic', padding: '6px 0' }}>
                      Processed
                    </span>
                  )}
                </Td>
              </Tr>
            ))}
          </Tbody>
        </Table>
      </div>
    </DashboardLayout>
  )
}

const registrationsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/registrations',
  component: () => (
    <AuthGuard>
      <RegistrationsComponent />
    </AuthGuard>
  )
})

// ==========================================
// 5. Config Component & Route (New!)
// ==========================================
interface ConfigVersionRecord {
  version: string
  status: 'PENDING' | 'ACKNOWLEDGED'
  clientAck?: string
  updatedAt: string
}

interface ConfigAuditRecord {
  id: string
  admin: string
  action: string
  target: string
  details: string
  timestamp: string
}

export const ConfigComponent: React.FC = () => {
  const { showToast } = useToast()
  
  // Automod forms state
  const [spamBlocker, setSpamBlocker] = useState(true)
  const [warningThreshold, setWarningThreshold] = useState(true)
  const [inviteBlocker, setInviteBlocker] = useState(false)
  const [spamThreshold, setSpamThreshold] = useState('5')

  // Bad words states
  const [badWordsString, setBadWordsString] = useState('hack, cheats, spammer, hacktools')
  const [newBadWord, setNewBadWord] = useState('')

  // Config Version and Audit Log states (Step 11)
  const [currentVersion, setCurrentVersion] = useState(1024)
  const [versions, setVersions] = useState<ConfigVersionRecord[]>([
    { version: 'v1024', status: 'ACKNOWLEDGED', clientAck: 'OficinaBot-Shard#0', updatedAt: '01:20:00' },
    { version: 'v1023', status: 'ACKNOWLEDGED', clientAck: 'OficinaBot-Shard#0', updatedAt: '01:10:00' },
    { version: 'v1022', status: 'ACKNOWLEDGED', clientAck: 'RegistrarService', updatedAt: '01:05:00' }
  ])
  const [auditLogs, setAuditLogs] = useState<ConfigAuditRecord[]>([
    { id: 'aud-01', admin: 'Leonardo#0001', action: 'CREATE', target: 'Automod Settings', details: 'Spam threshold rate set to 5', timestamp: '01:20:44' },
    { id: 'aud-02', admin: 'Leonardo#0001', action: 'UPDATE', target: 'Bad Words blocklist', details: 'Added blockword "cheats"', timestamp: '01:18:22' },
    { id: 'aud-03', admin: 'ModGuy#1232', action: 'UPDATE', target: 'Automod Settings', details: 'Anti-Spam Filter toggled ENABLED', timestamp: '01:10:12' }
  ])

  const handleSaveAutomod = () => {
    const nextVer = currentVersion + 1
    setCurrentVersion(nextVer)
    
    // Add pending config version
    const newVer: ConfigVersionRecord = {
      version: `v${nextVer}`,
      status: 'PENDING',
      updatedAt: new Date().toTimeString().split(' ')[0]
    }
    setVersions(prev => [newVer, ...prev])

    // Add audit entry
    const newAudit: ConfigAuditRecord = {
      id: `aud-${Math.random().toString(36).substring(2, 6)}`,
      admin: 'Leonardo#0001',
      action: 'UPDATE',
      target: 'Automod Settings',
      details: `Saved settings: Anti-spam: ${spamBlocker ? 'ON' : 'OFF'}, Spam limit: ${spamThreshold}`,
      timestamp: new Date().toTimeString().split(' ')[0]
    }
    setAuditLogs(prev => [newAudit, ...prev])

    showToast('Automod configurations saved successfully!', 'success')
  }

  const handleAddBadWord = (e: React.FormEvent) => {
    e.preventDefault()
    if (!newBadWord.trim()) {
      showToast('Bad word cannot be empty.', 'error')
      return
    }

    const currentWords = badWordsString.split(',').map(w => w.trim()).filter(Boolean)
    if (currentWords.includes(newBadWord.trim().toLowerCase())) {
      showToast('Word already present in block-pool.', 'warning')
      return
    }

    const wordToAdd = newBadWord.trim().toLowerCase()
    const updatedWords = [...currentWords, wordToAdd]
    setBadWordsString(updatedWords.join(', '))
    setNewBadWord('')

    const nextVer = currentVersion + 1
    setCurrentVersion(nextVer)

    // Add pending config version
    const newVer: ConfigVersionRecord = {
      version: `v${nextVer}`,
      status: 'PENDING',
      updatedAt: new Date().toTimeString().split(' ')[0]
    }
    setVersions(prev => [newVer, ...prev])

    // Add audit entry
    const newAudit: ConfigAuditRecord = {
      id: `aud-${Math.random().toString(36).substring(2, 6)}`,
      admin: 'Leonardo#0001',
      action: 'UPDATE',
      target: 'Bad Words blocklist',
      details: `Added blockword "${wordToAdd}"`,
      timestamp: new Date().toTimeString().split(' ')[0]
    }
    setAuditLogs(prev => [newAudit, ...prev])

    showToast(`Added blocked word: "${wordToAdd}"`, 'success')
  }

  const handleSaveBadWords = () => {
    const nextVer = currentVersion + 1
    setCurrentVersion(nextVer)

    // Add pending config version
    const newVer: ConfigVersionRecord = {
      version: `v${nextVer}`,
      status: 'PENDING',
      updatedAt: new Date().toTimeString().split(' ')[0]
    }
    setVersions(prev => [newVer, ...prev])

    // Add audit entry
    const newAudit: ConfigAuditRecord = {
      id: `aud-${Math.random().toString(36).substring(2, 6)}`,
      admin: 'Leonardo#0001',
      action: 'UPDATE',
      target: 'Bad Words blocklist',
      details: `Synchronized blocklist (length: ${badWordsString.length} chars)`,
      timestamp: new Date().toTimeString().split(' ')[0]
    }
    setAuditLogs(prev => [newAudit, ...prev])

    showToast('Bad words blocklist synchronized to local databases.', 'success')
  }

  const handleSyncToBot = () => {
    showToast('Broadcasting WAL configuration sync to OficinaBot clients...', 'info')
    
    // Simulate updating all PENDING config versions to ACKNOWLEDGED
    setTimeout(() => {
      setVersions(prev => prev.map(v => {
        if (v.status === 'PENDING') {
          return { ...v, status: 'ACKNOWLEDGED', clientAck: 'OficinaBot-Shard#0' }
        }
        return v
      }))
      showToast(`OficinaBot ACK: Synced configuration version ${currentVersion}!`, 'success')
    }, 1500)
  }

  const pendingCount = versions.filter(v => v.status === 'PENDING').length

  return (
    <DashboardLayout pageTitle="Automod Config">
      <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
        
        {/* Sync Actions Bar */}
        <div style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          backgroundColor: 'var(--bg-panel)',
          padding: '16px 20px',
          borderRadius: '8px',
          border: '1px solid var(--border-medium)'
        }}>
          <div>
            <div style={{ fontWeight: 'bold', fontSize: '15px' }}>
              Pending Configuration Updates
            </div>
            <div style={{ fontSize: '13px', marginTop: '4px' }}>
              {pendingCount > 0 ? (
                <span style={{ color: 'var(--color-warning)', fontWeight: '600' }}>
                  ⚠️ {pendingCount} update{pendingCount > 1 ? 's' : ''} awaiting sync propagation
                </span>
              ) : (
                <span style={{ color: 'var(--color-success)', fontWeight: '600' }}>
                  ✓ All changes synchronized to active bot clients
                </span>
              )}
            </div>
          </div>
          <Button onClick={handleSyncToBot}>
            Sync Config to Bots Now
          </Button>
        </div>

        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))',
          gap: '24px',
          alignItems: 'start'
        }}>
          {/* Form 1: Automod Core settings */}
          <div style={{
            backgroundColor: 'var(--bg-panel)',
            border: '1px solid var(--border-medium)',
            borderRadius: '8px',
            padding: '24px',
            display: 'flex',
            flexDirection: 'column',
            gap: '20px'
          }}>
            <h3 style={{ margin: 0, color: 'var(--text-primary)', fontSize: '16px', fontWeight: 'bold' }}>
              Core Automod Settings
            </h3>

            <FormField
              label="Anti-Spam Filter"
              helperText="Block messages that trigger rapid message-burst thresholds."
            >
              <div style={{ paddingTop: '4px' }}>
                <Switch
                  checked={spamBlocker}
                  onChange={(val) => setSpamBlocker(val)}
                  labelAfter={spamBlocker ? 'Enabled' : 'Disabled'}
                />
              </div>
            </FormField>

            <FormField
              label="Spam Message Threshold Rate"
              helperText="Maximum allowed messages per 3 seconds before timeout."
            >
              <Input
                type="number"
                value={spamThreshold}
                onChange={(e) => setSpamThreshold(e.target.value)}
                disabled={!spamBlocker}
              />
            </FormField>

            <FormField
              label="Auto-Timeout Warn Thresholds"
              helperText="Mutes member connection upon reaching 3 warning logs."
            >
              <div style={{ paddingTop: '4px' }}>
                <Switch
                  checked={warningThreshold}
                  onChange={(val) => setWarningThreshold(val)}
                  labelAfter={warningThreshold ? 'Active' : 'Muted'}
                />
              </div>
            </FormField>

            <FormField
              label="Restrict External Invites"
              helperText="Deletes all non-allowlisted Discord server invites."
            >
              <div style={{ paddingTop: '4px' }}>
                <Switch
                  checked={inviteBlocker}
                  onChange={(val) => setInviteBlocker(val)}
                  labelAfter={inviteBlocker ? 'Block All' : 'Permit'}
                />
              </div>
            </FormField>

            <div style={{ marginTop: '10px' }}>
              <Button onClick={handleSaveAutomod}>
                Save Automod Settings
              </Button>
            </div>
          </div>

          {/* Form 2: Blocked Bad Words list */}
          <div style={{
            backgroundColor: 'var(--bg-panel)',
            border: '1px solid var(--border-medium)',
            borderRadius: '8px',
            padding: '24px',
            display: 'flex',
            flexDirection: 'column',
            gap: '20px'
          }}>
            <h3 style={{ margin: 0, color: 'var(--text-primary)', fontSize: '16px', fontWeight: 'bold' }}>
              Bad Words Blocklist Pool
            </h3>

            <FormField label="Blocked Words list" helperText="Separated by comma. Moderation automatically deletes messages containing these words.">
              <Textarea
                value={badWordsString}
                onChange={(e) => setBadWordsString(e.target.value)}
                style={{ minHeight: '110px' }}
              />
            </FormField>

            <div style={{ display: 'flex', gap: '8px', marginTop: '10px' }}>
              <Button onClick={handleSaveBadWords}>
                Save Blocklist
              </Button>
            </div>

            <hr style={{ border: 'none', borderBottom: '1px solid var(--border-light)', margin: '10px 0' }} />

            <form onSubmit={handleAddBadWord} style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <FormField label="Register Single Blockword">
                <div style={{ display: 'flex', gap: '8px' }}>
                  <Input
                    value={newBadWord}
                    onChange={(e) => setNewBadWord(e.target.value)}
                    placeholder="e.g. bypassword"
                  />
                  <Button type="submit">
                    Add
                  </Button>
                </div>
              </FormField>
            </form>
          </div>
        </div>

        {/* Dual Panel Sync & Audit Monitor Section (Step 11) */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(360px, 1fr))',
          gap: '24px',
          alignItems: 'start'
        }}>
          
          {/* Column 1: Config Version Sync Monitor */}
          <div style={{
            backgroundColor: 'var(--bg-panel)',
            border: '1px solid var(--border-medium)',
            borderRadius: '8px',
            padding: '24px',
            display: 'flex',
            flexDirection: 'column',
            gap: '16px'
          }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <h3 style={{ margin: 0, color: 'var(--text-primary)', fontSize: '16px', fontWeight: 'bold' }}>
                Version Sync Monitor
              </h3>
              <span style={{
                fontSize: '11px',
                color: 'var(--color-info)',
                backgroundColor: 'rgba(59, 130, 246, 0.12)',
                padding: '3px 8px',
                borderRadius: '12px',
                fontWeight: '500'
              }}>
                Active Sync Mode
              </span>
            </div>
            
            <p style={{ margin: 0, fontSize: '12px', color: 'var(--text-secondary)', lineHeight: '1.5' }}>
              ℹ️ <strong>Propagation Delay:</strong> Configuration updates broadcast asynchronously. Offline or polling bot nodes synchronize local databases every <strong>5 minutes</strong>.
            </p>

            <Table>
              <Thead>
                <Tr>
                  <Th>Version</Th>
                  <Th>Sync Status</Th>
                  <Th>Acknowledge Client</Th>
                  <Th style={{ textAlign: 'right' }}>Time</Th>
                </Tr>
              </Thead>
              <Tbody>
                {versions.map((v) => (
                  <Tr key={v.version}>
                    <Td style={{ fontWeight: 'bold', color: 'var(--text-primary)' }}>{v.version}</Td>
                    <Td>
                      <span style={{
                        padding: '2px 6px',
                        borderRadius: '4px',
                        fontSize: '11px',
                        fontWeight: 'bold',
                        color: v.status === 'ACKNOWLEDGED' ? 'var(--color-success)' : 'var(--color-warning)',
                        backgroundColor: v.status === 'ACKNOWLEDGED' ? 'rgba(16, 185, 129, 0.15)' : 'rgba(245, 158, 11, 0.15)',
                        border: `1px solid ${v.status === 'ACKNOWLEDGED' ? 'rgba(16, 185, 129, 0.2)' : 'rgba(245, 158, 11, 0.2)'}`
                      }}>
                        {v.status === 'ACKNOWLEDGED' ? 'ACKED' : 'PENDING'}
                      </span>
                    </Td>
                    <Td style={{ fontFamily: 'monospace', fontSize: '12px' }}>
                      {v.clientAck ? (
                        <span style={{ color: 'var(--color-secondary)' }}>🤖 {v.clientAck}</span>
                      ) : (
                        <span style={{ color: 'var(--text-muted)', fontStyle: 'italic' }}>awaiting sync...</span>
                      )}
                    </Td>
                    <Td style={{ textAlign: 'right', color: 'var(--text-muted)', fontSize: '12px' }}>{v.updatedAt}</Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          </div>

          {/* Column 2: Configuration Audit Trail */}
          <div style={{
            backgroundColor: 'var(--bg-panel)',
            border: '1px solid var(--border-medium)',
            borderRadius: '8px',
            padding: '24px',
            display: 'flex',
            flexDirection: 'column',
            gap: '16px'
          }}>
            <h3 style={{ margin: 0, color: 'var(--text-primary)', fontSize: '16px', fontWeight: 'bold' }}>
              Configuration Audit Trail
            </h3>

            <Table>
              <Thead>
                <Tr>
                  <Th>Admin</Th>
                  <Th>Target</Th>
                  <Th>Change Description</Th>
                  <Th style={{ textAlign: 'right' }}>Time</Th>
                </Tr>
              </Thead>
              <Tbody>
                {auditLogs.map((log) => (
                  <Tr key={log.id}>
                    <Td style={{ fontWeight: 'bold', color: 'var(--text-primary)' }}>{log.admin}</Td>
                    <Td style={{ color: 'var(--color-secondary)', fontSize: '12px' }}>{log.target}</Td>
                    <Td style={{ fontSize: '12px', color: 'var(--text-secondary)' }}>{log.details}</Td>
                    <Td style={{ textAlign: 'right', color: 'var(--text-muted)', fontSize: '12px' }}>{log.timestamp}</Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          </div>
          
        </div>

      </div>
    </DashboardLayout>
  )
}

const configRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/config',
  component: () => (
    <AuthGuard>
      <ConfigComponent />
    </AuthGuard>
  )
})

// ==========================================
// Route Trees & Router initialization
// ==========================================
const adminRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/admin',
  component: () => (
    <AuthGuard>
      <AdminManagement />
    </AuthGuard>
  )
})

const loginRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/login',
  component: Login
})

// Create route tree co-registering all five dashboards + admin + login
const routeTree = rootRoute.addChildren([
  indexRoute,
  logsRoute,
  punishmentsRoute,
  registrationsRoute,
  configRoute,
  adminRoute,
  loginRoute
])

// Create router
export const router = createRouter({ routeTree })

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}
export default router
