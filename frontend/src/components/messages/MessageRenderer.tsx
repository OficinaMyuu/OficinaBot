import { memo } from "react"
import { useTranslation } from "react-i18next"
import type { TicketMessageView } from "@/types/ticket"
import { formatLocalTimestamp } from "@/utils/time"
import styles from "./MessageRenderer.module.css"

type MessageRendererProps = {
  messages: TicketMessageView[]
}

export const MessageRenderer = memo(function MessageRenderer({
  messages
}: MessageRendererProps) {
  const { t } = useTranslation()

  if (messages.length === 0) {
    return <div className={styles.empty}>{t("messages.empty")}</div>
  }

  return (
    <ol className={styles.list} aria-label={t("messages.logLabel")}>
      {messages.map((message) => {
        const isDeleted = message.is_deleted
        const content = message.content?.trim()

        return (
          <li
            className={[styles.message, isDeleted ? styles.deleted : null]
              .filter(Boolean)
              .join(" ")}
            key={message.message_id}
          >
            <img
              className={styles.avatar}
              src={message.author.avatar_url}
              alt=""
            />
            <div className={styles.body}>
              <div className={styles.meta}>
                <strong>{message.author.display_name}</strong>
                <time dateTime={message.created_at}>
                  {formatLocalTimestamp(message.created_at)}
                </time>
                {message.is_edited ? <span>{t("messages.edited")}</span> : null}
              </div>

              {message.message_reference_id ? (
                <div className={styles.reply}>
                  {t("messages.replyReference", {
                    id: message.message_reference_id
                  })}
                </div>
              ) : null}

              <p className={styles.content}>
                {isDeleted
                  ? t("messages.deleted")
                  : content
                    ? content
                    : t("messages.noTextContent")}
              </p>

              {message.sticker_id ? (
                <div className={styles.attachment}>
                  {t("messages.sticker", { id: message.sticker_id })}
                </div>
              ) : null}
              {message.deleted_by ? (
                <div className={styles.audit}>
                  {t("messages.deletedBy", {
                    name: message.deleted_by.display_name
                  })}
                </div>
              ) : null}
            </div>
          </li>
        )
      })}
    </ol>
  )
})
