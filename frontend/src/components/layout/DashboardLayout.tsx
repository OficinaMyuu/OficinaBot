import { Link } from "@tanstack/react-router"
import { useTranslation } from "react-i18next"
import { FaBirthdayCake } from "react-icons/fa"
import { FiDollarSign, FiLogOut, FiMenu, FiMessageSquare } from "react-icons/fi"
import { Group, Panel, Separator } from "react-resizable-panels"
import { useSession } from "@/contexts/SessionContext"
import { getEffectiveAvatarUrl } from "@/utils/userUtils"

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
      <Group orientation="horizontal" disableCursor>
        <Panel
          defaultSize={282}
          minSize={240}
          maxSize={400}
          className={styles.sidebarPanel}
        >
          <aside
            className={styles.sidebar}
            aria-label={t("navigation.dashboard")}
          >
            <div className={styles.brand}>
              {user?.guild_icon_url ? (
                <img
                  src={user.guild_icon_url}
                  alt={user.guild_name}
                  className={styles.brandIcon}
                />
              ) : (
                <div className={styles.brandMark}>
                  {user?.guild_name?.[0]?.toUpperCase() ?? "O"}
                </div>
              )}
              <div className={styles.brandText}>
                <strong>{user?.guild_name ?? "Oficina"}</strong>
                <span>{t("app.dashboard")}</span>
              </div>
            </div>

            <nav
              className={styles.modules}
              aria-label={t("navigation.dashboard")}
            >
              <section
                className={styles.moduleGroup}
                aria-labelledby="nav-misc"
              >
                <span className={styles.sectionLabel} id="nav-misc">
                  {t("navigation.categories.misc")}
                </span>
                <Link
                  to="/dashboard/birthdays"
                  className={styles.moduleLink}
                  activeProps={{
                    className: `${styles.moduleLink} ${styles.active}`
                  }}
                >
                  <FaBirthdayCake aria-hidden="true" />
                  <span>{t("birthdays.title")}</span>
                </Link>
              </section>

              <section
                className={styles.moduleGroup}
                aria-labelledby="nav-moderation"
              >
                <span className={styles.sectionLabel} id="nav-moderation">
                  {t("navigation.categories.moderation")}
                </span>
                <Link
                  to="/dashboard/tickets"
                  className={styles.moduleLink}
                  activeProps={{
                    className: `${styles.moduleLink} ${styles.active}`
                  }}
                >
                  <FiMessageSquare aria-hidden="true" />
                  <span>{t("tickets.title")}</span>
                </Link>
              </section>

              <section
                className={styles.moduleGroup}
                aria-labelledby="nav-economy"
              >
                <span className={styles.sectionLabel} id="nav-economy">
                  {t("navigation.categories.economy")}
                </span>
                <Link
                  to="/dashboard/economy/action-costs"
                  className={styles.moduleLink}
                  activeProps={{
                    className: `${styles.moduleLink} ${styles.active}`
                  }}
                >
                  <FiDollarSign aria-hidden="true" />
                  <span>{t("economy.actionCosts.title")}</span>
                </Link>
              </section>
            </nav>

            {user && (
              <div className={styles.profile}>
                <img
                  className={styles.avatar}
                  src={getEffectiveAvatarUrl(user)}
                  aria-hidden="true"
                />
                <div className={styles.profileText}>
                  <strong>{user?.global_name ?? user?.username}</strong>
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
            )}
          </aside>
        </Panel>

        <Separator className={styles.resizeHandle} />

        <Panel minSize={30}>
          <div className={styles.workspace}>
            <header className={styles.header}>
              <div className={styles.headerTitle}>
                <FiMenu aria-hidden="true" />
                <h1>{title}</h1>
              </div>
              <span className={styles.guild}>
                {user?.guild_name ?? "Oficina"}
              </span>
            </header>
            <main className={styles.content}>{children}</main>
          </div>
        </Panel>
      </Group>
    </div>
  )
}
