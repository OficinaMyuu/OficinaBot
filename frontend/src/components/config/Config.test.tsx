import { render, screen, fireEvent, act } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import React from 'react'
import { ConfigComponent } from '../../routes/router'
import { useAuth } from '../../context/AuthContext'
import { ToastProvider } from '../ui/Toast'

// Mock useAuth
vi.mock('../../context/AuthContext', () => ({
  useAuth: vi.fn()
}))

// Mock TanStack Router Link partially keeping original exports
vi.mock('@tanstack/react-router', async (importOriginal) => {
  const actual = await importOriginal<typeof import('@tanstack/react-router')>()
  return {
    ...actual,
    Link: ({ children, to, ...props }: { children: React.ReactNode; to?: string; [key: string]: unknown }) => (
      <a href={to} {...(props as React.AnchorHTMLAttributes<HTMLAnchorElement>)}>
        {children}
      </a>
    )
  }
})

describe('Config Component', () => {
  it('renders automod forms, bad words textarea, and triggers actions', () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: '1', username: 'Leonardo#0001', avatar: 'L', isOwner: true },
      isAuthenticated: true,
      isLoading: false,
      isForbidden: false,
      login: vi.fn(),
      logout: vi.fn()
    })

    render(
      <ToastProvider>
        <ConfigComponent />
      </ToastProvider>
    )

    expect(screen.getByText('Automod Config')).toBeInTheDocument()
    expect(screen.getByText('Core Automod Settings')).toBeInTheDocument()
    expect(screen.getByText('Bad Words Blocklist Pool')).toBeInTheDocument()
    
    // Assert Step 11 elements
    expect(screen.getByText('Version Sync Monitor')).toBeInTheDocument()
    expect(screen.getByText('Configuration Audit Trail')).toBeInTheDocument()
    expect(screen.getByText(/Propagation Delay:/i)).toBeInTheDocument()
    
    // Assert initial textarea block words
    const textarea = screen.getByDisplayValue('hack, cheats, spammer, hacktools')
    expect(textarea).toBeInTheDocument()
  })

  it('allows registering a new bad word through the form', () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: '1', username: 'Leonardo#0001', avatar: 'L', isOwner: true },
      isAuthenticated: true,
      isLoading: false,
      isForbidden: false,
      login: vi.fn(),
      logout: vi.fn()
    })

    render(
      <ToastProvider>
        <ConfigComponent />
      </ToastProvider>
    )

    const input = screen.getByPlaceholderText('e.g. bypassword')
    const addBtn = screen.getByRole('button', { name: /add/i })

    fireEvent.change(input, { target: { value: 'forbiddenword' } })
    fireEvent.click(addBtn)

    const textarea = screen.getByDisplayValue('hack, cheats, spammer, hacktools, forbiddenword')
    expect(textarea).toBeInTheDocument()

    // Assert Step 11 pending queue and audit updates
    expect(screen.getByText(/1 update awaiting sync propagation/i)).toBeInTheDocument()
    expect(screen.getByText('v1025')).toBeInTheDocument()
    expect(screen.getByText('Added blockword "forbiddenword"')).toBeInTheDocument()
  })

  it('handles manual bot synchronization and version acknowledgement transitions', () => {
    vi.useFakeTimers()
    vi.mocked(useAuth).mockReturnValue({
      user: { id: '1', username: 'Leonardo#0001', avatar: 'L', isOwner: true },
      isAuthenticated: true,
      isLoading: false,
      isForbidden: false,
      login: vi.fn(),
      logout: vi.fn()
    })

    render(
      <ToastProvider>
        <ConfigComponent />
      </ToastProvider>
    )

    // Save automod settings to queue a pending change
    const saveSettingsBtn = screen.getByRole('button', { name: /save automod settings/i })
    fireEvent.click(saveSettingsBtn)

    // Assert that we have a pending update awaiting sync
    expect(screen.getByText(/1 update awaiting sync propagation/i)).toBeInTheDocument()

    // Trigger sync
    const syncBtn = screen.getByRole('button', { name: /sync config to bots now/i })
    fireEvent.click(syncBtn)

    // Fast-forward timers to complete the asynchronous simulation
    act(() => {
      vi.runAllTimers()
    })

    // Assert that all changes are synchronized
    expect(screen.getByText(/All changes synchronized to active bot clients/i)).toBeInTheDocument()

    vi.useRealTimers()
  })
})
