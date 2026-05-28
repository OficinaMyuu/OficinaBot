import React from 'react'

export const Table: React.FC<React.TableHTMLAttributes<HTMLTableElement>> = ({
  children,
  style,
  ...props
}) => {
  return (
    <div
      style={{
        width: '100%',
        overflowX: 'auto',
        borderRadius: '8px',
        border: '1px solid var(--border-medium)',
        backgroundColor: 'var(--bg-panel)',
      }}
    >
      <table
        style={{
          width: '100%',
          borderCollapse: 'collapse',
          textAlign: 'left',
          fontSize: '14px',
          ...style,
        }}
        {...props}
      >
        {children}
      </table>
    </div>
  )
}

export const Thead: React.FC<React.HTMLAttributes<HTMLTableSectionElement>> = ({
  children,
  ...props
}) => {
  return (
    <thead style={{ backgroundColor: 'rgba(0, 0, 0, 0.15)' }} {...props}>
      {children}
    </thead>
  )
}

export const Tbody: React.FC<React.HTMLAttributes<HTMLTableSectionElement>> = ({
  children,
  ...props
}) => {
  return <tbody {...props}>{children}</tbody>
}

export const Th: React.FC<React.ThHTMLAttributes<HTMLTableCellElement>> = ({
  children,
  style,
  ...props
}) => {
  return (
    <th
      style={{
        padding: '14px 16px',
        color: 'var(--text-muted)',
        fontWeight: '700',
        fontSize: '11px',
        textTransform: 'uppercase',
        letterSpacing: '0.5px',
        borderBottom: '1px solid var(--border-medium)',
        ...style,
      }}
      {...props}
    >
      {children}
    </th>
  )
}

export const Td: React.FC<React.TdHTMLAttributes<HTMLTableCellElement>> = ({
  children,
  style,
  ...props
}) => {
  return (
    <td
      style={{
        padding: '14px 16px',
        color: 'var(--text-primary)',
        borderBottom: '1px solid var(--border-light)',
        fontSize: '13px',
        verticalAlign: 'middle',
        ...style,
      }}
      {...props}
    >
      {children}
    </td>
  )
}

export const Tr: React.FC<React.HTMLAttributes<HTMLTableRowElement>> = ({
  children,
  style,
  ...props
}) => {
  const [isHovered, setIsHovered] = React.useState(false)

  return (
    <tr
      onMouseEnter={() => setIsHovered(true)}
      onMouseLeave={() => setIsHovered(false)}
      style={{
        backgroundColor: isHovered ? 'var(--bg-hover)' : 'transparent',
        transition: 'background-color 0.15s cubic-bezier(0.4, 0, 0.2, 1)',
        ...style,
      }}
      {...props}
    >
      {children}
    </tr>
  )
}

export default Table
