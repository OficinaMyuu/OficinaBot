import { memo, useMemo } from "react"
import { useTranslation } from "react-i18next"
import type { MessageView } from "@/types/message"
import type { UserSummary } from "@/types/user"
import { MessageItem } from "./MessageItem"
import { isGroupedMessage } from "./messageGrouping"
import styles from "./MessageRenderer.module.css"

type MessageRendererProps = {
  channelId: string
  messages: MessageView[]
  usersById: Record<string, UserSummary>
  onMessageReferenceSelect?: (messageId: string) => void
}

export const MessageRenderer = memo(function MessageRenderer({
  channelId,
  messages,
  usersById,
  onMessageReferenceSelect
}: MessageRendererProps) {
  const { t } = useTranslation()
  const messagesById = useMemo(
    () => new Map(messages.map((message) => [message.message_id, message])),
    [messages]
  )

  if (messages.length === 0) {
    return <div className={styles.empty}>{t("messages.empty")}</div>
  }

  return (
    <ol className={styles.list} aria-label={t("messages.logLabel")}>
      {messages.map((message, index) => (
        <MessageItem
          key={message.message_id}
          channelId={channelId}
          message={message}
          usersById={usersById}
          referencedMessage={
            message.message_reference_id
              ? (messagesById.get(message.message_reference_id) ?? null)
              : null
          }
          grouped={isGroupedMessage(messages[index - 1], message)}
          onMessageReferenceSelect={onMessageReferenceSelect}
        />
      ))}
    </ol>
  )
})
