import type { SessionUser } from "@/types/session"

import { FiLogOut } from "react-icons/fi"
import { AppTooltip } from "@/components/ui/AppTooltip"
import { useTranslation } from "react-i18next"
import { getEffectiveAvatarUrl } from "@/utils/userUtils"

import styles from "./DashboardProfile.module.css"

type DashboardProfileProps = {
  onLogout: () => void
  user: SessionUser
}

export function DashboardProfile({ onLogout, user }: DashboardProfileProps) {
  const { t } = useTranslation()

  return (
    <div className={styles.profile}>
      <img className={styles.avatar} src={getEffectiveAvatarUrl(user)} alt="" />
      <div className={styles.text}>
        <strong>{user.global_name ?? user.username}</strong>
        <span>{t("auth.manageServer")}</span>
      </div>
      <AppTooltip label={t("auth.logout")}>
        <button
          className={styles.logout}
          type="button"
          aria-label={t("auth.logout")}
          onClick={onLogout}
        >
          <FiLogOut aria-hidden="true" />
        </button>
      </AppTooltip>
    </div>
  )
}
