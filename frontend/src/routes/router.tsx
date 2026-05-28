/* eslint-disable react-refresh/only-export-components */
import { createRootRoute, createRoute, createRouter, Outlet } from '@tanstack/react-router'
import React, { useState } from 'react'
import DashboardLayout from '../components/layout/DashboardLayout'
import Button from '../components/ui/Button'
import { Table, Thead, Tbody, Th, Td, Tr } from '../components/ui/Table'
import { Filters, SearchInput, Select } from '../components/ui/Filters'
import { FormField, Input, Switch, Textarea } from '../components/ui/Form'
import Modal from '../components/ui/Modal'
import { useToast } from '../components/ui/Toast'
import { LoadingState, EmptyState, ErrorState } from '../components/ui/FeedbackStates'

// Root route (outlet wrapper)
const rootRoute = createRootRoute({
  component: () => <Outlet />,
})

// ==========================================
// 1. Overview Component & Route
// ==========================================
const OverviewComponent: React.FC = () => {
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
        {/* Top row cards */}
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
            gap: '20px',
          }}
        >
          {/* Status Panel */}
          <div
            style={{
              backgroundColor: 'var(--bg-panel)',
              borderRadius: '8px',
              padding: '20px',
              border: '1px solid var(--border-medium)',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
              gap: '16px',
            }}
          >
            <div>
              <h3
                style={{
                  margin: '0 0 12px 0',
                  color: 'var(--color-secondary)',
                  fontSize: '14px',
                  textTransform: 'uppercase',
                  letterSpacing: '0.5px',
                }}
              >
                System Health & Integrity
              </h3>
              <div
                style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}
              >
                <span
                  style={{
                    width: '8px',
                    height: '8px',
                    borderRadius: '50%',
                    backgroundColor: 'var(--color-success)',
                    boxShadow: '0 0 8px var(--color-success)',
                  }}
                />
                <span style={{ fontSize: '15px', fontWeight: 'bold' }}>All systems active</span>
              </div>
              <span style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
                Latency: 12ms | CPU Load: 1.4% | Memory: 420MB / 1GB
              </span>
            </div>
            <div style={{ display: 'flex', gap: '10px' }}>
              <Button size="sm" onClick={() => setIsDiagnosticOpen(true)}>
                Run Diagnostics
              </Button>
              <Button
                variant="secondary"
                size="sm"
                onClick={() => showToast('Cache flushed successfully.', 'info')}
              >
                Flush Cache
              </Button>
            </div>
          </div>

          {/* Sync status card */}
          <div
            style={{
              backgroundColor: 'var(--bg-panel)',
              borderRadius: '8px',
              padding: '20px',
              border: '1px solid var(--border-medium)',
              display: 'flex',
              flexDirection: 'column',
              justifyContent: 'space-between',
              gap: '16px',
            }}
          >
            <div>
              <h3
                style={{
                  margin: '0 0 12px 0',
                  color: 'var(--color-secondary)',
                  fontSize: '14px',
                  textTransform: 'uppercase',
                  letterSpacing: '0.5px',
                }}
              >
                Gateway Connection
              </h3>
              <div
                style={{
                  fontSize: '15px',
                  fontWeight: 'bold',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '8px',
                  marginBottom: '8px',
                }}
              >
                <span>🤖</span>
                <span>OficinaBot (Main Server)</span>
              </div>
              <span style={{ fontSize: '13px', color: 'var(--text-secondary)' }}>
                Sync token active. Shard #0 running on Hostsquare.
              </span>
            </div>
            <div>
              <Button
                variant="danger"
                size="sm"
                onClick={() => showToast('Reconnection requested to shard #0', 'warning')}
              >
                Reconnect Shard
              </Button>
            </div>
          </div>
        </div>

        {/* Quick Settings Form Panel */}
        <div
          style={{
            backgroundColor: 'var(--bg-panel)',
            border: '1px solid var(--border-medium)',
            borderRadius: '8px',
            padding: '24px',
          }}
        >
          <h3
            style={{
              margin: '0 0 20px 0',
              fontSize: '18px',
              fontWeight: 'bold',
              color: 'var(--text-primary)',
            }}
          >
            Quick Settings Configuration
          </h3>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px', maxWidth: '600px' }}>
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

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '20px' }}>
              <FormField
                label="Max User Reminders Limit"
                helperText="Maximum allowed concurrent reminders."
              >
                <Input
                  type="number"
                  value={maxReminders}
                  onChange={(e) => setMaxReminders(e.target.value)}
                />
              </FormField>

              <FormField
                label="Maintenance Mode"
                helperText="Blocks all non-admin interactions."
                style={{ justifyContent: 'center' }}
              >
                <div style={{ paddingTop: '8px' }}>
                  <Switch
                    checked={maintenanceMode}
                    onChange={(val) => {
                      setMaintenanceMode(val)
                      showToast(
                        `Maintenance mode is now ${val ? 'ENABLED' : 'DISABLED'}`,
                        val ? 'warning' : 'info',
                      )
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
      </div>

      {/* Diagnostic Modal */}
      <Modal
        isOpen={isDiagnosticOpen}
        onClose={() => !isDiagnosticsRunning && setIsDiagnosticOpen(false)}
        title="Diagnostic Suite"
        footer={
          <>
            <Button
              variant="secondary"
              disabled={isDiagnosticsRunning}
              onClick={() => setIsDiagnosticOpen(false)}
            >
              Close
            </Button>
            <Button isLoading={isDiagnosticsRunning} onClick={runDiagnostics}>
              Execute Integrity Checks
            </Button>
          </>
        }
      >
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <p style={{ margin: 0 }}>
            This suite executes real-time validation checks against the bot gateway, SQLite
            read/write thresholds, and active memory allocation loops.
          </p>
          <div
            style={{
              backgroundColor: 'rgba(0,0,0,0.2)',
              border: '1px solid var(--border-medium)',
              borderRadius: '6px',
              padding: '12px',
              fontFamily: 'monospace',
              fontSize: '12px',
            }}
          >
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
  component: OverviewComponent,
})

// ==========================================
// 2. Logs Component & Route
// ==========================================
const mockLogs = [
  {
    id: '1',
    level: 'info',
    author: 'Leonardo#0001',
    channel: 'geral',
    message: 'User updated custom status',
    time: '01:10:22',
  },
  {
    id: '2',
    level: 'info',
    author: 'OficinaBot',
    channel: 'logs',
    message: 'Synced bad words configuration',
    time: '01:12:45',
  },
  {
    id: '3',
    level: 'warning',
    author: 'RandomGamer#2341',
    channel: 'geral',
    message: 'Automod flagged message: spam link pattern matched',
    time: '01:14:12',
  },
  {
    id: '4',
    level: 'error',
    author: 'SystemError',
    channel: 'core',
    message: 'Discord REST request failed: rate limit hit on guild member list',
    time: '01:15:33',
  },
]

const LogsComponent: React.FC = () => {
  const { showToast } = useToast()
  const [search, setSearch] = useState('')
  const [levelFilter, setLevelFilter] = useState('all')
  const [isLoading, setIsLoading] = useState(false)

  const handleClearSearch = () => {
    setSearch('')
    showToast('Cleared search filters.', 'info')
  }

  const filteredLogs = mockLogs.filter((log) => {
    const matchesSearch =
      log.message.toLowerCase().includes(search.toLowerCase()) ||
      log.author.toLowerCase().includes(search.toLowerCase())
    const matchesLevel = levelFilter === 'all' || log.level === levelFilter
    return matchesSearch && matchesLevel
  })

  const getLevelStyle = (level: string): React.CSSProperties => {
    switch (level) {
      case 'success':
        return {
          color: 'var(--color-success)',
          backgroundColor: 'rgba(16, 185, 129, 0.15)',
          border: '1px solid rgba(16, 185, 129, 0.3)',
        }
      case 'error':
        return {
          color: 'var(--color-danger)',
          backgroundColor: 'rgba(239, 68, 68, 0.15)',
          border: '1px solid rgba(239, 68, 68, 0.3)',
        }
      case 'warning':
        return {
          color: 'var(--color-warning)',
          backgroundColor: 'rgba(245, 158, 11, 0.15)',
          border: '1px solid rgba(245, 158, 11, 0.3)',
        }
      case 'info':
      default:
        return {
          color: 'var(--color-info)',
          backgroundColor: 'rgba(59, 130, 246, 0.15)',
          border: '1px solid rgba(59, 130, 246, 0.3)',
        }
    }
  }

  const simulateLoading = () => {
    setIsLoading(true)
    setTimeout(() => {
      setIsLoading(false)
      showToast('Logs log-pool refreshed!', 'success')
    }, 1500)
  }

  return (
    <DashboardLayout pageTitle="System Logs">
      <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
        {/* Filters Bar */}
        <Filters>
          <SearchInput
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search logs by message or username..."
            onClear={handleClearSearch}
          />

          <Select
            label="Level"
            value={levelFilter}
            onChange={(e) => setLevelFilter(e.target.value)}
          >
            <option value="all">All Levels</option>
            <option value="info">Info</option>
            <option value="warning">Warning</option>
            <option value="error">Error</option>
          </Select>

          <Button variant="secondary" onClick={simulateLoading}>
            Refresh Logs
          </Button>
        </Filters>

        {/* Feedback states */}
        {isLoading ? (
          <LoadingState />
        ) : filteredLogs.length === 0 ? (
          <EmptyState
            title="No logs found"
            description="No logging records matched your active filter values. Try clearing text or selecting all levels."
            icon="🔎"
            actionLabel="Reset Search"
            onAction={handleClearSearch}
          />
        ) : (
          /* Table Data */
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
              {filteredLogs.map((log) => (
                <Tr key={log.id}>
                  <Td style={{ fontFamily: 'monospace', color: 'var(--text-muted)' }}>
                    {log.time}
                  </Td>
                  <Td>
                    <span
                      style={{
                        padding: '2px 8px',
                        borderRadius: '10px',
                        fontSize: '11px',
                        fontWeight: 'bold',
                        textTransform: 'uppercase',
                        ...getLevelStyle(log.level),
                      }}
                    >
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
        )}
      </div>
    </DashboardLayout>
  )
}

const logsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/logs',
  component: LogsComponent,
})

// ==========================================
// 3. Punishments Component & Route
// ==========================================
interface PunishmentRecord {
  id: string
  user: string
  type: string
  reason: string
  date: string
}

const initialPunishments: PunishmentRecord[] = [
  {
    id: '1',
    user: 'BadUser#1111',
    type: 'BAN',
    reason: 'Toxicity & slurs in geral channel',
    date: 'May 25, 2026',
  },
  {
    id: '2',
    user: 'SpammyBot#4321',
    type: 'KICK',
    reason: 'Automated token invite links spamming',
    date: 'May 27, 2026',
  },
]

const PunishmentsComponent: React.FC = () => {
  const { showToast } = useToast()
  const [punishments, setPunishments] = useState<PunishmentRecord[]>(initialPunishments)
  const [user, setUser] = useState('')
  const [type, setType] = useState('WARN')
  const [reason, setReason] = useState('')
  const [formError, setFormError] = useState<{ user?: string; reason?: string }>({})

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
      id: Math.random().toString(36).substring(2, 9),
      user,
      type,
      reason,
      date: 'Today',
    }

    setPunishments([newRecord, ...punishments])
    setUser('')
    setReason('')
    showToast(`Punishment issued successfully: ${type} against ${user}`, 'success')
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

        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))',
            gap: '24px',
            alignItems: 'start',
          }}
        >
          {/* Action Form */}
          <form
            onSubmit={handleIssuePunishment}
            style={{
              backgroundColor: 'var(--bg-panel)',
              border: '1px solid var(--border-medium)',
              borderRadius: '8px',
              padding: '20px',
              display: 'flex',
              flexDirection: 'column',
              gap: '16px',
            }}
          >
            <h3
              style={{
                margin: 0,
                color: 'var(--text-primary)',
                fontSize: '16px',
                fontWeight: 'bold',
              }}
            >
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
                  cursor: 'pointer',
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
            <h3
              style={{
                margin: 0,
                color: 'var(--text-primary)',
                fontSize: '16px',
                fontWeight: 'bold',
              }}
            >
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
                    <Th>Reason</Th>
                  </Tr>
                </Thead>
                <Tbody>
                  {punishments.map((p) => (
                    <Tr key={p.id}>
                      <Td style={{ fontWeight: 'bold' }}>{p.user}</Td>
                      <Td>
                        <span
                          style={{
                            padding: '2px 6px',
                            borderRadius: '4px',
                            fontSize: '11px',
                            fontWeight: 'bold',
                            backgroundColor:
                              p.type === 'BAN'
                                ? 'rgba(239, 68, 68, 0.15)'
                                : 'rgba(245, 158, 11, 0.15)',
                            color:
                              p.type === 'BAN' ? 'var(--color-danger)' : 'var(--color-warning)',
                            border: `1px solid ${p.type === 'BAN' ? 'rgba(239, 68, 68, 0.2)' : 'rgba(245, 158, 11, 0.2)'}`,
                          }}
                        >
                          {p.type}
                        </span>
                      </Td>
                      <Td style={{ color: 'var(--text-secondary)' }}>{p.reason}</Td>
                    </Tr>
                  ))}
                </Tbody>
              </Table>
            )}
          </div>
        </div>
      </div>
    </DashboardLayout>
  )
}

const punishmentsRoute = createRoute({
  getParentRoute: () => rootRoute,
  path: '/punishments',
  component: PunishmentsComponent,
})

// Create route tree
const routeTree = rootRoute.addChildren([indexRoute, logsRoute, punishmentsRoute])

// Create router
export const router = createRouter({ routeTree })

declare module '@tanstack/react-router' {
  interface Register {
    router: typeof router
  }
}
export default router
