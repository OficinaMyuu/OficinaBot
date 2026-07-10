import { useTranslation } from "react-i18next"
import styles from "./Notice.module.css"

type NoticeProps = {
  message: string | null
  onDismiss: () => void
}

export function Notice({ message, onDismiss }: NoticeProps) {
  const { t } = useTranslation()

  if (!message) return null

  return (
    <div className={styles.notice} role="status">
      <span>{message}</span>
      <button type="button" onClick={onDismiss}>
        {t("common.dismiss")}
      </button>
    </div>
  )
}
