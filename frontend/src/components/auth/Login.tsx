import React, { useState } from 'react'
import { useAuth } from '../../context/AuthContext'
import Button from '../ui/Button'
import { useToast } from '../ui/Toast'

export const Login: React.FC = () => {
  const { login } = useAuth()
  const { showToast } = useToast()
  const [loadingType, setLoadingType] = useState<'owner' | 'moderator' | 'forbidden' | null>(null)

  const handleLogin = async (type: 'owner' | 'moderator' | 'forbidden') => {
    setLoadingType(type)
    showToast(`Redirecting to Discord authorization for ${type}...`, 'info')

    try {
      await login(type)
      if (type !== 'forbidden') {
        showToast('Successfully logged in! Session activated.', 'success')
      } else {
        showToast('Discord credentials rejected: not in allowlist.', 'error')
      }
    } catch {
      showToast('Authentication connection failed.', 'error')
    } finally {
      setLoadingType(null)
    }
  }

  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '100vh',
        backgroundColor: '#07050e', // Very deep black-purple
        color: '#f3f0ff',
        fontFamily: 'system-ui, -apple-system, sans-serif',
        padding: '24px',
        boxSizing: 'border-box',
        position: 'relative',
        overflow: 'hidden',
      }}
    >
      {/* Visual background glows */}
      <div
        style={{
          position: 'absolute',
          width: '500px',
          height: '500px',
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(124, 58, 237, 0.15) 0%, transparent 70%)',
          top: '-10%',
          left: '-10%',
          pointerEvents: 'none',
        }}
      />
      <div
        style={{
          position: 'absolute',
          width: '600px',
          height: '600px',
          borderRadius: '50%',
          background: 'radial-gradient(circle, rgba(192, 132, 252, 0.1) 0%, transparent 70%)',
          bottom: '-20%',
          right: '-10%',
          pointerEvents: 'none',
        }}
      />

      {/* Main card panel */}
      <div
        style={{
          maxWidth: '440px',
          width: '100%',
          backgroundColor: '#110e24',
          border: '1px solid #2d2254',
          borderRadius: '12px',
          padding: '40px 32px',
          textAlign: 'center',
          boxShadow: '0 25px 50px -12px rgba(0,0,0,0.5)',
          zIndex: 5,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          gap: '24px',
        }}
        className="animate-slide-in"
      >
        {/* Branding Icon */}
        <div
          style={{
            width: '64px',
            height: '64px',
            borderRadius: '16px',
            background: 'linear-gradient(135deg, #7c3aed, #c084fc)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '28px',
            boxShadow: '0 8px 24px rgba(124, 58, 237, 0.3)',
          }}
        >
          🤖
        </div>

        <div>
          <h1
            style={{ margin: '0 0 8px 0', fontSize: '24px', fontWeight: 'bold', color: '#f3f0ff' }}
          >
            OficinaServices
          </h1>
          <p style={{ margin: 0, fontSize: '14px', color: '#94a3b8', lineHeight: '1.5' }}>
            Enter your Discord credentials to access the guild's administrative console.
          </p>
        </div>

        <div
          style={{
            width: '100%',
            height: '1px',
            backgroundColor: '#2d2254',
          }}
        />

        {/* CTA Login Buttons */}
        <div
          style={{
            display: 'flex',
            flexDirection: 'column',
            gap: '12px',
            width: '100%',
          }}
        >
          {/* Owner option */}
          <Button
            isLoading={loadingType === 'owner'}
            disabled={loadingType !== null}
            onClick={() => handleLogin('owner')}
            style={{
              padding: '12px',
              fontSize: '14px',
              width: '100%',
              background: 'linear-gradient(135deg, #5865F2, #4752C4)', // Discord Blue
              boxShadow: '0 4px 12px rgba(88, 101, 242, 0.25)',
            }}
          >
            👾 Login with Discord (Owner)
          </Button>

          {/* Moderator option */}
          <Button
            isLoading={loadingType === 'moderator'}
            disabled={loadingType !== null}
            variant="secondary"
            onClick={() => handleLogin('moderator')}
            style={{ width: '100%', padding: '12px' }}
          >
            🛡️ Login as Moderator
          </Button>

          {/* Forbidden option */}
          <Button
            isLoading={loadingType === 'forbidden'}
            disabled={loadingType !== null}
            variant="ghost"
            onClick={() => handleLogin('forbidden')}
            style={{ width: '100%', color: '#64748b', fontSize: '13px' }}
          >
            Simulate Allowlist Rejection
          </Button>
        </div>
      </div>
    </div>
  )
}

export default Login
