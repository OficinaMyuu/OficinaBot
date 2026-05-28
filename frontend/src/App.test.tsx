import { render, screen } from '@testing-library/react'
import { describe, it, expect, vi } from 'vitest'
import App from './App'
import { useAuth } from './context/AuthContext'

vi.mock('./context/AuthContext', () => ({
  useAuth: vi.fn(),
  AuthProvider: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}))

describe('App Component', () => {
  it('renders app overview layout successfully', async () => {
    vi.mocked(useAuth).mockReturnValue({
      user: { id: '1', username: 'Leonardo#0001', avatar: 'L', isOwner: true },
      isAuthenticated: true,
      isLoading: false,
      isForbidden: false,
      login: vi.fn(),
      logout: vi.fn(),
    })

    render(<App />)
    expect(await screen.findByText('OficinaServices')).toBeInTheDocument()
    expect(screen.getByText('OficinaBot: Synced')).toBeInTheDocument()
  })
})
