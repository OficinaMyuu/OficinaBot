import { Link } from "@tanstack/react-router"
import { useTranslation } from "react-i18next"
import { FaBirthdayCake } from "react-icons/fa"
import { FiDollarSign, FiMessageSquare } from "react-icons/fi"
import styles from "./DashboardNavigation.module.css"

export function DashboardNavigation() {
  const { t } = useTranslation()

  return (
    <nav className={styles.navigation} aria-label={t("navigation.dashboard")}>
      <section className={styles.group} aria-labelledby="nav-misc">
        <span className={styles.label} id="nav-misc">
          {t("navigation.categories.misc")}
        </span>
        <Link
          to="/dashboard/birthdays"
          className={styles.link}
          activeProps={{ className: `${styles.link} ${styles.active}` }}
        >
          <FaBirthdayCake aria-hidden="true" />
          <span>{t("birthdays.title")}</span>
        </Link>
      </section>

      <section className={styles.group} aria-labelledby="nav-moderation">
        <span className={styles.label} id="nav-moderation">
          {t("navigation.categories.moderation")}
        </span>
        <Link
          to="/dashboard/tickets"
          className={styles.link}
          activeProps={{ className: `${styles.link} ${styles.active}` }}
        >
          <FiMessageSquare aria-hidden="true" />
          <span>{t("tickets.title")}</span>
        </Link>
      </section>

      <section className={styles.group} aria-labelledby="nav-economy">
        <span className={styles.label} id="nav-economy">
          {t("navigation.categories.economy")}
        </span>
        <Link
          to="/dashboard/economy/action-costs"
          className={styles.link}
          activeProps={{ className: `${styles.link} ${styles.active}` }}
        >
          <FiDollarSign aria-hidden="true" />
          <span>{t("economy.actionCosts.title")}</span>
        </Link>
      </section>
    </nav>
  )
}
