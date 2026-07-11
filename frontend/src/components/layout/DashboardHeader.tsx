import { FiMenu } from "react-icons/fi"
import { useSession } from "@/contexts/SessionContext"
import styles from "./DashboardHeader.module.css"

type DashboardHeaderProps = {
  title: string
}

export function DashboardHeader({ title }: DashboardHeaderProps) {
  const { user } = useSession()

  return (
    <header className={styles.header}>
      <div className={styles.title}>
        <FiMenu aria-hidden="true" />
        <h1>{title}</h1>
      </div>
      <span className={styles.guild}>{user?.guild_name ?? "Oficina"}</span>
    </header>
  )
}
