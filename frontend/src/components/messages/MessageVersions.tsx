import { useTranslation } from "react-i18next"
import type { MessageVersion } from "@/types/message"
import { formatMessageTimestamp } from "@/utils/timeUtils"
import styles from "./MessageVersions.module.css"

type MessageVersionsProps = {
  versions: MessageVersion[]
  loading: boolean
  error: boolean
}

export function MessageVersions({
  versions,
  loading,
  error
}: MessageVersionsProps) {
  const { t } = useTranslation()
  if (loading)
    return (
      <div className={styles.versions}>{t("messages.loadingVersions")}</div>
    )
  if (error)
    return <div className={styles.versions}>{t("messages.versionsError")}</div>
  return (
    <ol className={styles.versions}>
      {versions.slice(0, -1).map((version) => (
        <li key={version.created_at}>
          <time dateTime={version.created_at}>
            {formatMessageTimestamp(version.created_at)}
          </time>
          <span>{version.content?.trim() || t("messages.noTextContent")}</span>
        </li>
      ))}
    </ol>
  )
}
