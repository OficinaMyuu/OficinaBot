import React from 'react'

export interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost'
  size?: 'sm' | 'md' | 'lg'
  isLoading?: boolean
}

export const Button: React.FC<ButtonProps> = ({
  children,
  variant = 'primary',
  size = 'md',
  isLoading = false,
  style,
  disabled,
  ...props
}) => {
  // Harmonies based on Discord's dark/purple palette
  const getVariantStyles = (): React.CSSProperties => {
    switch (variant) {
      case 'secondary':
        return {
          backgroundColor: '#393053',
          color: '#f3f0ff',
          border: '1px solid #443c68',
        }
      case 'danger':
        return {
          backgroundColor: '#dc2626',
          color: '#ffffff',
          border: 'none',
        }
      case 'ghost':
        return {
          backgroundColor: 'transparent',
          color: '#94a3b8',
          border: 'none',
        }
      case 'primary':
      default:
        return {
          background: 'linear-gradient(135deg, #7c3aed, #6d28d9)',
          color: '#ffffff',
          border: 'none',
          boxShadow: '0 4px 12px rgba(109, 40, 217, 0.25)',
        }
    }
  }

  const getSizeStyles = (): React.CSSProperties => {
    switch (size) {
      case 'sm':
        return {
          padding: '6px 12px',
          fontSize: '13px',
          borderRadius: '4px',
        }
      case 'lg':
        return {
          padding: '12px 24px',
          fontSize: '16px',
          borderRadius: '8px',
        }
      case 'md':
      default:
        return {
          padding: '8px 16px',
          fontSize: '14px',
          borderRadius: '6px',
        }
    }
  }

  const baseStyles: React.CSSProperties = {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontWeight: 'bold',
    cursor: disabled || isLoading ? 'not-allowed' : 'pointer',
    opacity: disabled || isLoading ? 0.6 : 1,
    transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
    fontFamily: 'system-ui, -apple-system, sans-serif',
    outline: 'none',
    userSelect: 'none',
    ...getVariantStyles(),
    ...getSizeStyles(),
    ...style,
  }

  return (
    <button disabled={disabled || isLoading} style={baseStyles} {...props}>
      {isLoading ? (
        <span style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <span
            style={{
              width: '14px',
              height: '14px',
              border: '2px solid rgba(255, 255, 255, 0.3)',
              borderTopColor: '#ffffff',
              borderRadius: '50%',
              animation: 'spin 0.8s linear infinite',
            }}
          />
          Loading...
        </span>
      ) : (
        children
      )}
    </button>
  )
}

export default Button
