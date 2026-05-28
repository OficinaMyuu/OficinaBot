import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import AuthGuard from './AuthGuard'
import { useAuth } from '../../context/AuthContext'
import { ToastProvider } from '../ui/Toast'

// Mock useAuth
vi.mock('../../context/AuthContext', () => ({
  useAuth: vi.fn(),
}))

describe('AuthGuard Component', () => {
  it('renders loading view when session is loading', () => {
    vi.mocked(useAuth).mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoading: true,
      isForbidden: false,
      login: vi.fn(),
      logout: vi.fn(),
    })

    render(
      <AuthGuard>
        <div>Secret Content</div>
      </AuthGuard>,
    )
    expect(screen.getByText('Validating admin credentials...')).toBeInTheDocument()
    expect(screen.queryByText('Secret Content')).not.toBeInTheDocument()
  })

  it('renders forbidden warning when account is blocked', () => {
    vi.mocked(useAuth).mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      isForbidden: true,
      login: vi.fn(),
      logout: vi.fn(),
    })

    render(
      <ToastProvider>
        <AuthGuard>
          <div>Secret Content</div>
        </AuthGuard>
      </ToastProvider>,
    )
    expect(screen.getByText('Access Restrained')).toBeInTheDocument()
    expect(screen.getByText(/allowlist table/i)).toBeInTheDocument()
    expect(screen.queryByText('Secret Content')).not.toBeInTheDocument()
  })

  it('renders login view when not authenticated', () => {
    vi.mocked(useAuth).mockReturnValue({
      user: null,
      isAuthenticated: false,
      isLoading: false,
      isForbidden: false,
      login: vi.fn(),
      logout: vi.fn(),
    })

    render(
      <ToastProvider>
        <AuthGuard>
          <div>Secret Content</div>
        </AuthGuard>
      </ToastProvider>,
    )
    expect(screen.getByText('OficinaServices')).toBeInTheDocument()
    expect(screen.getByText(/Login with Discord/i)).toBeInTheDocument()
    expect(screen.queryByText('Secret Content')).not.toBeInTheDocument()
  })

  it('renders child contents when successfully authenticated', () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: '1', username: 'TestUser', avatar: 'T', isOwner: false },
      isAuthenticated: true,
      isLoading: false,
      isForbidden: false,
      login: vi.fn(),
      logout: vi.fn(),
    })

    render(
      <AuthGuard>
        <div>Secret Content</div>
      </AuthGuard>,
    )
    expect(screen.getByText('Secret Content')).toBeInTheDocument()
  })
})
