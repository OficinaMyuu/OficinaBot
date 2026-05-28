import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import React from 'react'
import AdminManagement from './AdminManagement'
import { useAuth } from '../../context/AuthContext'
import { ToastProvider } from '../ui/Toast'

// Mock useAuth
vi.mock('../../context/AuthContext', () => ({
  useAuth: vi.fn(),
}))

// Mock TanStack Router Link to avoid context issues in unit testing
vi.mock('@tanstack/react-router', () => ({
  Link: ({
    children,
    to,
    ...props
  }: {
    children: React.ReactNode
    to?: string
    [key: string]: unknown
  }) => (
    <a href={to} {...(props as React.AnchorHTMLAttributes<HTMLAnchorElement>)}>
      {children}
    </a>
  ),
}))

describe('AdminManagement Component', () => {
  it('blocks access and renders access restrained error if user is NOT the owner', () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: '2', username: 'Moderator#0001', avatar: 'M', isOwner: false },
      isAuthenticated: true,
      isLoading: false,
      isForbidden: false,
      login: vi.fn(),
      logout: vi.fn(),
    })

    render(
      <ToastProvider>
        <AdminManagement />
      </ToastProvider>,
    )

    expect(screen.getByText('Access Restrained')).toBeInTheDocument()
    expect(screen.getByText(/strictly reserved for the Primary Server Owner/i)).toBeInTheDocument()
    expect(screen.queryByText('Allowlist New Admin')).not.toBeInTheDocument()
  })

  it('renders allowlist page and forms if user IS the owner', () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: '1', username: 'Leonardo#0001', avatar: 'L', isOwner: true },
      isAuthenticated: true,
      isLoading: false,
      isForbidden: false,
      login: vi.fn(),
      logout: vi.fn(),
    })

    render(
      <ToastProvider>
        <AdminManagement />
      </ToastProvider>,
    )

    expect(screen.getByText('Allowlist New Admin')).toBeInTheDocument()
    expect(screen.getByText('Allowlisted Administrators')).toBeInTheDocument()
    expect(screen.getAllByText('Leonardo#0001').length).toBeGreaterThan(0)
  })
})
