import styles from "./DataTableSkeleton.module.css"

type DataTableSkeletonProps = {
  columns: number
  label: string
  rows?: number
}

export function DataTableSkeleton({
  columns,
  label,
  rows = 4
}: DataTableSkeletonProps) {
  return (
    <div className={styles.skeleton} role="status" aria-label={label}>
      {Array.from({ length: rows }, (_, rowIndex) => (
        <div
          className={styles.row}
          key={rowIndex}
          style={{
            gridTemplateColumns: `repeat(${columns}, minmax(100px, 1fr))`
          }}
        >
          {Array.from({ length: columns }, (_, columnIndex) => (
            <span className={styles.cell} key={columnIndex} />
          ))}
        </div>
      ))}
    </div>
  )
}
