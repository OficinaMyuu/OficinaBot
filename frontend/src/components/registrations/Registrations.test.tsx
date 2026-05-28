import { render, screen, fireEvent } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import React from 'react'
import { RegistrationsComponent } from '../../routes/router'
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

describe('Registrations Component', () => {
  it('renders registered member tag name and mock actions', () => {
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
        <RegistrationsComponent />
      </ToastProvider>
    )

    expect(screen.getByText('Member Registrations')).toBeInTheDocument()
    expect(screen.getByText('MemberSpike#2211')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /approve/i })).toBeInTheDocument()
  })

  it('handles approve action correctly', () => {
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
        <RegistrationsComponent />
      </ToastProvider>
    )

    const approveBtn = screen.getByRole('button', { name: /approve/i })
    fireEvent.click(approveBtn)

    expect(screen.getAllByText('APPROVED')).toHaveLength(2)
    expect(screen.queryByRole('button', { name: /approve/i })).not.toBeInTheDocument()
  })
})
