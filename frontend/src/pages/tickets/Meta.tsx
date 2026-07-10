import type { ReactNode } from "react"
import clsx from "clsx"
import styles from "./Meta.module.css"

type MetaProps = {
  label: string
  value: ReactNode
  mono?: boolean
}

export function Meta({ label, value, mono = false }: MetaProps) {
  return (
    <div className={styles.metaRow}>
      <dt className={styles.metaLabel}>{label}</dt>
      <dd className={clsx(styles.metaData, mono && styles.mono)}>{value}</dd>
    </div>
  )
}
