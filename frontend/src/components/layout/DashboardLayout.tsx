import { Link } from "@tanstack/react-router"
import { useTranslation } from "react-i18next"
import { FaBirthdayCake } from "react-icons/fa"
import { FiLogOut, FiMenu } from "react-icons/fi"
import { useSession } from "@/contexts/SessionContext"
import styles from "./DashboardLayout.module.css"

type DashboardLayoutProps = {
  children: React.ReactNode
  title: string
}

export function DashboardLayout({ children, title }: DashboardLayoutProps) {
  const { t } = useTranslation()
  const { user, logout } = useSession()

  return (
    <div className={styles.shell}>
      <aside className={styles.sidebar} aria-label={t("navigation.modules")}>
        <div className={styles.brand}>
          {user?.guildIconUrl ? (
            <img
              src={user.guildIconUrl}
              alt={user.guildName}
              className={styles.brandIcon}
            />
          ) : (
            <div className={styles.brandMark}>
              {user?.guildName?.[0]?.toUpperCase() ?? "O"}
            </div>
          )}
          <div className={styles.brandText}>
            <strong>{user?.guildName ?? "Oficina"}</strong>
            <span>{t("app.dashboard")}</span>
          </div>
        </div>

        <nav className={styles.modules}>
          <span className={styles.sectionLabel}>{t("navigation.modules")}</span>
          <Link
            to="/dashboard/birthdays"
            className={styles.moduleLink}
            activeProps={{ className: `${styles.moduleLink} ${styles.active}` }}
          >
            <FaBirthdayCake aria-hidden="true" />
            <span>{t("birthdays.title")}</span>
          </Link>
        </nav>

        <div className={styles.profile}>
          <div className={styles.avatar} aria-hidden="true">
            {user?.globalName?.[0] ?? user?.username?.[0] ?? "O"}
          </div>
          <div className={styles.profileText}>
            <strong>{user?.globalName ?? user?.username}</strong>
            <span>{t("auth.manageServer")}</span>
          </div>
          <button
            className={styles.iconButton}
            type="button"
            onClick={() => void logout()}
            title={t("auth.logout")}
          >
            <FiLogOut aria-hidden="true" />
          </button>
        </div>
      </aside>

      <div className={styles.workspace}>
        <header className={styles.header}>
          <div className={styles.headerTitle}>
            <FiMenu aria-hidden="true" />
            <h1>{title}</h1>
          </div>
          <span className={styles.guild}>{user?.guildName ?? "Oficina"}</span>
        </header>
        <main className={styles.content}>{children}</main>
      </div>
    </div>
  )
}
