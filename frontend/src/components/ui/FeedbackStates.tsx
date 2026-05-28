import React from 'react'
import Button from './Button'

// 1. LoadingState Component (Skeleton pulses)
export const LoadingState: React.FC = () => {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '16px', width: '100%' }}>
      {/* Header pulse */}
      <div
        style={{
          height: '28px',
          width: '35%',
          backgroundColor: 'var(--bg-hover)',
          borderRadius: '6px',
          animation: 'pulse 1.5s infinite ease-in-out',
        }}
      />

      {/* Description pulse */}
      <div
        style={{
          height: '16px',
          width: '60%',
          backgroundColor: 'var(--bg-hover)',
          borderRadius: '4px',
          animation: 'pulse 1.5s infinite ease-in-out',
        }}
      />

      {/* Card grids pulses */}
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))',
          gap: '20px',
          marginTop: '10px',
        }}
      >
        {[1, 2, 3].map((n) => (
          <div
            key={n}
            style={{
              backgroundColor: 'var(--bg-panel)',
              borderRadius: '8px',
              padding: '24px',
              border: '1px solid var(--border-medium)',
              display: 'flex',
              flexDirection: 'column',
              gap: '12px',
            }}
          >
            <div
              style={{
                height: '18px',
                width: '45%',
                backgroundColor: 'var(--bg-hover)',
                borderRadius: '4px',
                animation: 'pulse 1.5s infinite ease-in-out',
              }}
            />
            <div
              style={{
                height: '14px',
                width: '80%',
                backgroundColor: 'var(--bg-hover)',
                borderRadius: '4px',
                animation: 'pulse 1.5s infinite ease-in-out',
              }}
            />
            <div
              style={{
                height: '32px',
                width: '30%',
                backgroundColor: 'var(--bg-hover)',
                borderRadius: '6px',
                marginTop: '8px',
                animation: 'pulse 1.5s infinite ease-in-out',
              }}
            />
          </div>
        ))}
      </div>

      {/* Pulse Keyframe declaration */}
      <style>{`
        @keyframes pulse {
          0% { opacity: 0.6; }
          50% { opacity: 0.3; }
          100% { opacity: 0.6; }
        }
      `}</style>
    </div>
  )
}

// 2. EmptyState Component
interface EmptyStateProps {
  title?: string
  description?: string
  actionLabel?: string
  onAction?: () => void
  icon?: string
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  title = 'No records found',
  description = 'There are no items to show right now. Try expanding your search or adding a new record.',
  actionLabel,
  onAction,
  icon = '📁',
}) => {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '48px 24px',
        textAlign: 'center',
        backgroundColor: 'var(--bg-panel)',
        border: '1px dashed var(--border-medium)',
        borderRadius: '8px',
        color: 'var(--text-secondary)',
      }}
    >
      <span style={{ fontSize: '48px', marginBottom: '16px' }}>{icon}</span>
      <h3
        style={{
          margin: '0 0 8px 0',
          fontSize: '18px',
          color: 'var(--text-primary)',
          fontWeight: 'bold',
        }}
      >
        {title}
      </h3>
      <p style={{ margin: '0 0 20px 0', fontSize: '14px', maxWidth: '380px', lineHeight: '1.5' }}>
        {description}
      </p>
      {actionLabel && onAction && <Button onClick={onAction}>{actionLabel}</Button>}
    </div>
  )
}

// 3. ErrorState Component
interface ErrorStateProps {
  title?: string
  message?: string
  onRetry?: () => void
}

export const ErrorState: React.FC<ErrorStateProps> = ({
  title = 'Something went wrong',
  message = 'An error occurred while loading the data from the services.',
  onRetry,
}) => {
  return (
    <div
      style={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '36px 24px',
        textAlign: 'center',
        backgroundColor: 'rgba(239, 68, 68, 0.05)',
        border: '1px solid rgba(239, 68, 68, 0.2)',
        borderRadius: '8px',
        color: 'var(--color-danger)',
      }}
    >
      <span style={{ fontSize: '32px', marginBottom: '12px' }}>⚠️</span>
      <h3 style={{ margin: '0 0 8px 0', fontSize: '16px', fontWeight: 'bold' }}>{title}</h3>
      <p
        style={{
          margin: '0 0 16px 0',
          fontSize: '13px',
          color: 'var(--text-secondary)',
          maxWidth: '400px',
          lineHeight: '1.5',
        }}
      >
        {message}
      </p>
      {onRetry && (
        <Button
          variant="secondary"
          onClick={onRetry}
          style={{ borderColor: 'rgba(239, 68, 68, 0.4)' }}
        >
          Retry Connection
        </Button>
      )}
    </div>
  )
}
