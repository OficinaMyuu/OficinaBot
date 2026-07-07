/* eslint-disable react-refresh/only-export-components */
import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import { authService } from '@/services/authService'
import type { SessionUser } from '@/types/session'

type SessionState = {
  user: SessionUser | null
  isLoading: boolean
  error: string | null
  refresh: () => Promise<void>
  login: () => void
  logout: () => Promise<void>
}

const SessionContext = createContext<SessionState | null>(null)

export function SessionProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<SessionUser | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refresh = useCallback(async () => {
    setIsLoading(true)
    setError(null)
    try {
      const session = await authService.getSession()
      setUser(session.user)
    } catch (err) {
      const status = typeof err === 'object' && err !== null && 'status' in err ? err.status : undefined
      if (status !== 401) {
        setError(err instanceof Error ? err.message : 'Failed to load session')
      }
      setUser(null)
    } finally {
      setIsLoading(false)
    }
  }, [])

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      void refresh()
    }, 0)
    return () => window.clearTimeout(timeoutId)
  }, [refresh])

  const logout = useCallback(async () => {
    await authService.logout()
    setUser(null)
  }, [])

  const value = useMemo<SessionState>(
    () => ({
      user,
      isLoading,
      error,
      refresh,
      login: authService.login,
      logout,
    }),
    [error, isLoading, logout, refresh, user],
  )

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>
}

export function useSession(): SessionState {
  const value = useContext(SessionContext)
  if (!value) {
    throw new Error('useSession must be used within SessionProvider')
  }
  return value
}
