import { useTranslation } from "react-i18next"
import styles from "./DashboardBrand.module.css"

type DashboardBrandProps = {
  guildIconUrl?: string | null
  guildName?: string | null
}

export function DashboardBrand({
  guildIconUrl,
  guildName
}: DashboardBrandProps) {
  const { t } = useTranslation()
  const displayName = guildName ?? "Oficina"

  return (
    <div className={styles.brand}>
      {guildIconUrl ? (
        <img src={guildIconUrl} alt={displayName} className={styles.icon} />
      ) : (
        <div className={styles.mark}>
          {displayName[0]?.toUpperCase() ?? "O"}
        </div>
      )}
      <div className={styles.text}>
        <strong>{displayName}</strong>
        <span>{t("app.dashboard")}</span>
      </div>
    </div>
  )
}
