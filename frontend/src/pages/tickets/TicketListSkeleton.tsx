import styles from "./TicketListSkeleton.module.css"

type TicketListSkeletonProps = {
  label: string
}

export function TicketListSkeleton({ label }: TicketListSkeletonProps) {
  return (
    <ol className={styles.entries} aria-label={label} aria-busy="true">
      {[0, 1, 2, 3].map((item) => (
        <li className={styles.entry} key={item}>
          <span className={styles.chevron} />
          <span className={styles.avatar} />
          <span className={styles.title} />
          <span className={styles.user} />
          <span className={styles.status} />
          <span className={styles.timestamp} />
        </li>
      ))}
    </ol>
  )
}
