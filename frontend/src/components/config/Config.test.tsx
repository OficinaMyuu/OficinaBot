import { render, screen, fireEvent } from '@testing-library/react'
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
  })
})
