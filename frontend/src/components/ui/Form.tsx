import React from 'react'

interface FormFieldProps {
  label: string
  error?: string
  helperText?: string
  children: React.ReactNode
  style?: React.CSSProperties
}

export const FormField: React.FC<FormFieldProps> = ({
  label,
  error,
  helperText,
  children,
  style,
}) => {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', width: '100%', ...style }}>
      <label
        style={{
          fontSize: '12px',
          fontWeight: 'bold',
          textTransform: 'uppercase',
          color: error ? 'var(--color-danger)' : 'var(--text-secondary)',
          letterSpacing: '0.5px',
        }}
      >
        {label}
      </label>
      {children}
      {error ? (
        <span style={{ fontSize: '12px', color: 'var(--color-danger)', marginTop: '2px' }}>
          ⚠️ {error}
        </span>
      ) : helperText ? (
        <span style={{ fontSize: '12px', color: 'var(--text-muted)', marginTop: '2px' }}>
          {helperText}
        </span>
      ) : null}
    </div>
  )
}

export const Input: React.FC<
  React.InputHTMLAttributes<HTMLInputElement> & { hasError?: boolean }
> = ({ hasError, style, ...props }) => {
  return (
    <input
      style={{
        width: '100%',
        padding: '10px 14px',
        backgroundColor: 'var(--bg-input)',
        border: `1px solid ${hasError ? 'var(--color-danger)' : 'var(--border-medium)'}`,
        borderRadius: '6px',
        color: 'var(--text-primary)',
        fontSize: '14px',
        outline: 'none',
        transition: 'all 0.15s ease',
        boxSizing: 'border-box',
        ...style,
      }}
      onFocus={(e) => {
        if (!hasError) {
          e.currentTarget.style.borderColor = 'var(--border-focus)'
          e.currentTarget.style.boxShadow = '0 0 0 2px rgba(124, 58, 237, 0.15)'
        }
      }}
      onBlur={(e) => {
        if (!hasError) {
          e.currentTarget.style.borderColor = 'var(--border-medium)'
          e.currentTarget.style.boxShadow = 'none'
        }
      }}
      {...props}
    />
  )
}

export const Textarea: React.FC<
  React.TextareaHTMLAttributes<HTMLTextAreaElement> & { hasError?: boolean }
> = ({ hasError, style, ...props }) => {
  return (
    <textarea
      style={{
        width: '100%',
        padding: '10px 14px',
        backgroundColor: 'var(--bg-input)',
        border: `1px solid ${hasError ? 'var(--color-danger)' : 'var(--border-medium)'}`,
        borderRadius: '6px',
        color: 'var(--text-primary)',
        fontSize: '14px',
        outline: 'none',
        transition: 'all 0.15s ease',
        minHeight: '80px',
        resize: 'vertical',
        fontFamily: 'inherit',
        boxSizing: 'border-box',
        ...style,
      }}
      onFocus={(e) => {
        if (!hasError) {
          e.currentTarget.style.borderColor = 'var(--border-focus)'
          e.currentTarget.style.boxShadow = '0 0 0 2px rgba(124, 58, 237, 0.15)'
        }
      }}
      onBlur={(e) => {
        if (!hasError) {
          e.currentTarget.style.borderColor = 'var(--border-medium)'
          e.currentTarget.style.boxShadow = 'none'
        }
      }}
      {...props}
    />
  )
}

interface SwitchProps extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'onChange'> {
  checked: boolean
  onChange: (checked: boolean) => void
  labelAfter?: string
}

export const Switch: React.FC<SwitchProps> = ({
  checked,
  onChange,
  labelAfter,
  disabled,
  style,
  ...props
}) => {
  return (
    <label
      style={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '10px',
        cursor: disabled ? 'not-allowed' : 'pointer',
        opacity: disabled ? 0.6 : 1,
        userSelect: 'none',
        ...style,
      }}
    >
      <div style={{ position: 'relative', width: '40px', height: '20px' }}>
        <input
          type="checkbox"
          checked={checked}
          onChange={(e) => !disabled && onChange(e.target.checked)}
          style={{
            opacity: 0,
            width: 0,
            height: 0,
            margin: 0,
            position: 'absolute',
          }}
          disabled={disabled}
          {...props}
        />
        {/* Slide track */}
        <div
          style={{
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: checked ? 'var(--color-primary)' : 'var(--border-medium)',
            borderRadius: '20px',
            transition: 'background-color 0.2s ease',
          }}
        />
        {/* Sliding Knob */}
        <div
          style={{
            position: 'absolute',
            width: '14px',
            height: '14px',
            left: checked ? '23px' : '3px',
            bottom: '3px',
            backgroundColor: '#ffffff',
            borderRadius: '50%',
            transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
            boxShadow: '0 1px 3px rgba(0,0,0,0.3)',
          }}
        />
      </div>
      {labelAfter && (
        <span style={{ fontSize: '14px', fontWeight: '500', color: 'var(--text-secondary)' }}>
          {labelAfter}
        </span>
      )}
    </label>
  )
}
export default Input
