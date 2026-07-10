import styles from './SortableHeader.module.css'

type SortableHeaderProps = {
  label: string
  sortKey: string
  activeSortKey: string | null
  sortDir: 'asc' | 'desc'
  onSort: (key: string) => void
}

export function SortableHeader({
  label,
  sortKey,
  activeSortKey,
  sortDir,
  onSort
}: SortableHeaderProps) {
  const isActive = sortKey === activeSortKey

  return (
    <th
      className={styles.header}
      onClick={() => onSort(sortKey)}
      aria-sort={isActive ? (sortDir === 'asc' ? 'ascending' : 'descending') : undefined}
    >
      <span className={styles.content}>
        {label}
        <span className={styles.indicator} aria-hidden="true">
          {isActive ? (sortDir === 'asc' ? '▲' : '▼') : '⇅'}
        </span>
      </span>
    </th>
  )
}
