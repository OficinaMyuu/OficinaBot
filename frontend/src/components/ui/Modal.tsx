import React, { useEffect } from 'react'
import Button from './Button'

interface ModalProps {
  isOpen: boolean
  onClose: () => void
  title: string
  children: React.ReactNode
  footer?: React.ReactNode
  size?: 'sm' | 'md' | 'lg'
}

export const Modal: React.FC<ModalProps> = ({
  isOpen,
  onClose,
  title,
  children,
  footer,
  size = 'md',
}) => {
  // Prevent body scrolling when modal is open
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = 'hidden'
    } else {
      document.body.style.overflow = ''
    }
    return () => {
      document.body.style.overflow = ''
    }
  }, [isOpen])

  if (!isOpen) return null

  const getWidth = (): string => {
    switch (size) {
      case 'sm':
        return '400px'
      case 'lg':
        return '700px'
      case 'md':
      default:
        return '520px'
    }
  }

  return (
    <div
      onClick={onClose}
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(7, 5, 14, 0.75)', // Deep black overlay
        backdropFilter: 'blur(4px)',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        zIndex: 100,
        padding: '20px',
        boxSizing: 'border-box',
      }}
      className="animate-fadeIn"
    >
      {/* Modal Dialog Content Container */}
      <div
        onClick={(e) => e.stopPropagation()}
        style={{
          width: '100%',
          maxWidth: getWidth(),
          backgroundColor: 'var(--bg-panel)',
          border: '1px solid var(--border-medium)',
          borderRadius: '10px',
          boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.5), 0 10px 10px -5px rgba(0, 0, 0, 0.4)',
          display: 'flex',
          flexDirection: 'column',
          maxHeight: 'calc(100vh - 40px)',
          overflow: 'hidden',
        }}
        className="animate-slide-in"
      >
        {/* Modal Header */}
        <div
          style={{
            padding: '16px 20px',
            borderBottom: '1px solid var(--border-medium)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            backgroundColor: 'rgba(0, 0, 0, 0.1)',
          }}
        >
          <h3
            style={{
              margin: 0,
              fontSize: '18px',
              color: 'var(--text-primary)',
              fontWeight: 'bold',
            }}
          >
            {title}
          </h3>
          <button
            onClick={onClose}
            style={{
              background: 'none',
              border: 'none',
              color: 'var(--text-muted)',
              cursor: 'pointer',
              padding: '6px',
              fontSize: '16px',
              lineHeight: 1,
              outline: 'none',
              transition: 'color 0.15s',
            }}
            onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--text-primary)')}
            onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--text-muted)')}
          >
            ✕
          </button>
        </div>

        {/* Modal Scrollable Body */}
        <div
          style={{
            padding: '20px',
            overflowY: 'auto',
            color: 'var(--text-secondary)',
            fontSize: '14px',
            lineHeight: '1.5',
          }}
        >
          {children}
        </div>

        {/* Modal Footer Controls */}
        <div
          style={{
            padding: '16px 20px',
            borderTop: '1px solid var(--border-medium)',
            display: 'flex',
            justifyContent: 'flex-end',
            gap: '10px',
            backgroundColor: 'rgba(0, 0, 0, 0.1)',
          }}
        >
          {footer ? (
            footer
          ) : (
            <>
              <Button variant="secondary" onClick={onClose}>
                Cancel
              </Button>
              <Button onClick={() => alert('Confirmed!')}>Confirm</Button>
            </>
          )}
        </div>
      </div>
    </div>
  )
}

export default Modal
