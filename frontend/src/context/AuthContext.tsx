/* eslint-disable react-refresh/only-export-components */
import React, { createContext, useContext, useState, useCallback, useEffect } from 'react'

export interface AuthUser {
  id: string
  username: string
  avatar: string
  isOwner: boolean
}

interface AuthContextType {
  user: AuthUser | null
  isAuthenticated: boolean
  isLoading: boolean
  isForbidden: boolean
  login: (type: 'owner' | 'moderator' | 'forbidden') => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<AuthUser | null>(() => {
    const savedUser = localStorage.getItem('oficina_session_user')
    return savedUser ? JSON.parse(savedUser) : null
  })
  const [isForbidden, setIsForbidden] = useState<boolean>(() => {
    return localStorage.getItem('oficina_session_forbidden') === 'true'
  })
  const [isLoading, setIsLoading] = useState(true)

  // Validate active sessions on mount
  useEffect(() => {
    // Simulate minor gateway handshake loading
    const timer = setTimeout(() => {
      setIsLoading(false)
    }, 800)

    return () => clearTimeout(timer)
  }, [])

  const login = useCallback(async (type: 'owner' | 'moderator' | 'forbidden') => {
    setIsLoading(true)
    setIsForbidden(false)

    // Simulate Discord OAuth2 redirect lag
    await new Promise((resolve) => setTimeout(resolve, 1000))

    if (type === 'forbidden') {
      setIsForbidden(true)
      setUser(null)
      localStorage.setItem('oficina_session_forbidden', 'true')
      localStorage.removeItem('oficina_session_user')
    } else {
      const isOwner = type === 'owner'
      const mockUser: AuthUser = {
        id: isOwner ? '1337' : '9999',
        username: isOwner ? 'Leonardo#0001' : 'Moderator#0001',
        avatar: isOwner ? 'L' : 'M',
        isOwner,
      }
      setUser(mockUser)
      setIsForbidden(false)
      localStorage.setItem('oficina_session_user', JSON.stringify(mockUser))
      localStorage.removeItem('oficina_session_forbidden')
    }
    setIsLoading(false)
  }, [])

  const logout = useCallback(async () => {
    setIsLoading(true)
    await new Promise((resolve) => setTimeout(resolve, 500))
    setUser(null)
    setIsForbidden(false)
    localStorage.removeItem('oficina_session_user')
    localStorage.removeItem('oficina_session_forbidden')
    setIsLoading(false)
  }, [])

  return (
    <AuthContext.Provider
      value={{
        user,
        isAuthenticated: !!user,
        isLoading,
        isForbidden,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
