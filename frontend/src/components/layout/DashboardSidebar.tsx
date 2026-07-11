import { useTranslation } from "react-i18next"
import { useSession } from "@/contexts/SessionContext"
import { DashboardBrand } from "./DashboardBrand"
import { DashboardNavigation } from "./DashboardNavigation"
import { DashboardProfile } from "./DashboardProfile"
import styles from "./DashboardSidebar.module.css"

export function DashboardSidebar() {
  const { t } = useTranslation()
  const { logout, user } = useSession()

  return (
    <aside className={styles.sidebar} aria-label={t("navigation.dashboard")}>
      <DashboardBrand
        guildIconUrl={user?.guild_icon_url}
        guildName={user?.guild_name}
      />
      <DashboardNavigation />
      {user ? (
        <DashboardProfile user={user} onLogout={() => void logout()} />
      ) : null}
    </aside>
  )
}
