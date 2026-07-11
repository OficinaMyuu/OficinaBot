import styles from "./DashboardAccessSkeleton.module.css"

export function DashboardAccessSkeleton() {
  return (
    <main className={styles.root} aria-busy="true">
      <div className={styles.card}>
        <span className={styles.mark} />
        <span className={styles.title} />
        <span className={styles.copy} />
      </div>
    </main>
  )
}
