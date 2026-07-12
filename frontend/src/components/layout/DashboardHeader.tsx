import { FiMenu } from "react-icons/fi"

import styles from "./DashboardHeader.module.css"

type DashboardHeaderProps = {
  title: string
}

export function DashboardHeader({ title }: DashboardHeaderProps) {
  return (
    <header className={styles.header}>
      <div className={styles.title}>
        <FiMenu aria-hidden="true" />
        <h1>{title}</h1>
      </div>
    </header>
  )
}
