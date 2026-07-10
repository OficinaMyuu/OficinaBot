import styles from "./MessageSkeleton.module.css"

export function MessageSkeleton({ label }: { label: string }) {
  return (
    <div className={styles.skeleton} role="status" aria-label={label}>
      {[0, 1, 2].map((item) => (
        <div className={styles.skeletonRow} key={item}>
          <span />
          <div>
            <strong />
            <p />
          </div>
        </div>
      ))}
    </div>
  )
}
