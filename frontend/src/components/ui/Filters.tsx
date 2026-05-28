import React from 'react'

interface FiltersProps {
  children: React.ReactNode
}

export const Filters: React.FC<FiltersProps> = ({ children }) => {
  return (
    <div
      style={{
        display: 'flex',
        flexWrap: 'wrap',
        gap: '12px',
        alignItems: 'center',
        padding: '16px',
        backgroundColor: 'var(--bg-panel)',
        border: '1px solid var(--border-medium)',
        borderRadius: '8px',
        marginBottom: '20px',
      }}
    >
      {children}
    </div>
  )
}

interface SearchInputProps extends React.InputHTMLAttributes<HTMLInputElement> {
  onClear?: () => void
}

export const SearchInput: React.FC<SearchInputProps> = ({ onClear, style, ...props }) => {
  return (
    <div
      style={{
        position: 'relative',
        display: 'flex',
        alignItems: 'center',
        flex: 1,
        minWidth: '200px',
      }}
    >
      <input
        type="text"
        style={{
          width: '100%',
          padding: '8px 36px 8px 12px',
          backgroundColor: 'var(--bg-input)',
          border: '1px solid var(--border-medium)',
          borderRadius: '6px',
          color: 'var(--text-primary)',
          fontSize: '14px',
          outline: 'none',
          transition: 'all 0.2s',
          ...style,
        }}
        onFocus={(e) => {
          e.currentTarget.style.borderColor = 'var(--border-focus)'
          e.currentTarget.style.boxShadow = '0 0 0 2px rgba(124, 58, 237, 0.2)'
        }}
        onBlur={(e) => {
          e.currentTarget.style.borderColor = 'var(--border-medium)'
          e.currentTarget.style.boxShadow = 'none'
        }}
        {...props}
      />
      <span
        style={{
          position: 'absolute',
          right: '12px',
          color: 'var(--text-muted)',
          fontSize: '14px',
          pointerEvents: 'none',
        }}
      >
        🔍
      </span>
      {props.value && onClear && (
        <button
          onClick={onClear}
          style={{
            position: 'absolute',
            right: '32px',
            background: 'none',
            border: 'none',
            color: 'var(--text-muted)',
            cursor: 'pointer',
            padding: '4px',
            fontSize: '12px',
          }}
        >
          ✕
        </button>
      )}
    </div>
  )
}

interface SelectProps extends React.SelectHTMLAttributes<HTMLSelectElement> {
  label?: string
}

export const Select: React.FC<SelectProps> = ({ label, children, style, ...props }) => {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
      {label && (
        <span style={{ fontSize: '13px', color: 'var(--text-secondary)', fontWeight: '500' }}>
          {label}:
        </span>
      )}
      <select
        style={{
          padding: '8px 24px 8px 12px',
          backgroundColor: 'var(--bg-input)',
          border: '1px solid var(--border-medium)',
          borderRadius: '6px',
          color: 'var(--text-primary)',
          fontSize: '14px',
          outline: 'none',
          cursor: 'pointer',
          appearance: 'none',
          backgroundImage:
            'linear-gradient(45deg, transparent 50%, var(--text-muted) 50%), linear-gradient(135deg, var(--text-muted) 50%, transparent 50%)',
          backgroundPosition: 'calc(100% - 15px) 50%, calc(100% - 10px) 50%',
          backgroundSize: '5px 5px, 5px 5px',
          backgroundRepeat: 'no-repeat',
          transition: 'all 0.2s',
          ...style,
        }}
        onFocus={(e) => {
          e.currentTarget.style.borderColor = 'var(--border-focus)'
        }}
        onBlur={(e) => {
          e.currentTarget.style.borderColor = 'var(--border-medium)'
        }}
        {...props}
      >
        {children}
      </select>
    </div>
  )
}

export default Filters
