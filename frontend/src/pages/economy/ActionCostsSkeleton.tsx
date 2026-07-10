import styles from "./ActionCostsSkeleton.module.css"

type ActionCostsSkeletonProps = {
  label: string
}

export function ActionCostsSkeleton({ label }: ActionCostsSkeletonProps) {
  return (
    <div className={styles.skeleton} role="status" aria-label={label}>
      {[0, 1, 2, 3].map((row) => (
        <div className={styles.skeletonRow} key={row}>
          <span />
          <span />
          <span />
          <span />
        </div>
      ))}
    </div>
  )
}
