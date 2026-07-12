import { Button } from "@/components/ui/Button"
import { useTranslation } from "react-i18next"
import { FiRefreshCw } from "react-icons/fi"

import styles from "./ActionCostsHeader.module.css"

type ActionCostsHeaderProps = {
  isRefreshing: boolean
  onRefresh: () => void
}

export function ActionCostsHeader({
  isRefreshing,
  onRefresh
}: ActionCostsHeaderProps) {
  const { t } = useTranslation()

  return (
    <header className={styles.heading}>
      <div>
        <h2 id="action-costs-title">{t("economy.actionCosts.title")}</h2>
        <p>{t("economy.actionCosts.description")}</p>
      </div>
      <Button
        type="button"
        variant="secondary"
        disabled={isRefreshing}
        onClick={onRefresh}
      >
        <FiRefreshCw aria-hidden="true" />
        {t("common.refresh")}
      </Button>
    </header>
  )
}
