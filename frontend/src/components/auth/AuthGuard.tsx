import React from 'react'
import { useAuth } from '../../context/AuthContext'
import Login from './Login'
import Button from '../ui/Button'

interface AuthGuardProps {
  children: React.ReactNode
}

export const AuthGuard: React.FC<AuthGuardProps> = ({ children }) => {
  const { isAuthenticated, isLoading, isForbidden, logout } = useAuth()

  // 1. Loading Session Gate
  if (isLoading) {
    return (
      <div
        style={{
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '100vh',
          backgroundColor: '#07050e',
          color: '#f3f0ff',
          fontFamily: 'system-ui, -apple-system, sans-serif',
          gap: '16px',
        }}
      >
        <div
          style={{
            width: '40px',
            height: '40px',
            border: '3px solid rgba(124, 58, 237, 0.2)',
            borderTopColor: 'var(--color-primary)',
            borderRadius: '50%',
            animation: 'spin 1s linear infinite',
          }}
        />
        <span
          style={{
            fontSize: '14px',
            color: 'var(--text-secondary)',
            fontWeight: '500',
            letterSpacing: '0.5px',
          }}
        >
          Validating admin credentials...
        </span>
      </div>
    )
  }

  // 2. Forbidden Access Allowlist Rejection Gate
  if (isForbidden) {
    return (
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          minHeight: '100vh',
          backgroundColor: '#07050e',
          color: '#f3f0ff',
          fontFamily: 'system-ui, -apple-system, sans-serif',
          padding: '24px',
        }}
      >
        <div
          style={{
            maxWidth: '460px',
            width: '100%',
            backgroundColor: '#110e24',
            border: '1px solid rgba(239, 68, 68, 0.2)',
            borderRadius: '12px',
            padding: '40px 32px',
            textAlign: 'center',
            boxShadow: '0 25px 50px -12px rgba(0,0,0,0.5)',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: '20px',
          }}
          className="animate-slide-in"
        >
          <div
            style={{
              width: '56px',
              height: '56px',
              borderRadius: '50%',
              backgroundColor: 'rgba(239, 68, 68, 0.1)',
              color: 'var(--color-danger)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: '28px',
              border: '2px solid rgba(239, 68, 68, 0.2)',
            }}
          >
            ⚠️
          </div>

          <div>
            <h2
              style={{
                margin: '0 0 10px 0',
                fontSize: '20px',
                fontWeight: 'bold',
                color: 'var(--color-danger)',
              }}
            >
              Access Restrained
            </h2>
            <p
              style={{
                margin: 0,
                fontSize: '14px',
                color: 'var(--text-secondary)',
                lineHeight: '1.6',
              }}
            >
              Your Discord ID is authenticated but not registered inside the OficinaServices
              administrator allowlist table.
            </p>
          </div>

          <div
            style={{
              padding: '12px 16px',
              backgroundColor: 'rgba(0,0,0,0.2)',
              borderRadius: '6px',
              fontSize: '12px',
              color: 'var(--text-muted)',
              textAlign: 'left',
              width: '100%',
              lineHeight: '1.5',
            }}
          >
            🔑 Request allowlisting from the server owner (<strong>Leonardo#0001</strong>) using the
            slash user register control commands.
          </div>

          <Button
            variant="secondary"
            onClick={logout}
            style={{ width: '100%', borderColor: 'rgba(239, 68, 68, 0.3)', color: '#ef4444' }}
          >
            ← Return to Login screen
          </Button>
        </div>
      </div>
    )
  }

  // 3. Logged-Out/Unauthenticated Gate
  if (!isAuthenticated) {
    return <Login />
  }

  // 4. Authenticated state
  return <>{children}</>
}

export default AuthGuard
