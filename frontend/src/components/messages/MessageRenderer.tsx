import type { MessageView } from "@/types/message"
import type { UserSummary } from "@/types/user"

import { memo, useMemo } from "react"
import { useTranslation } from "react-i18next"
import { MessageGroup } from "./MessageGroup"
import { createMessageGroups } from "./messageGrouping"

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
  const messageGroups = useMemo(() => createMessageGroups(messages), [messages])

  if (messages.length === 0) {
    return <div className={styles.empty}>{t("messages.empty")}</div>
  }

  return (
    <ol className={styles.list} aria-label={t("messages.logLabel")}>
      {messageGroups.map((group) => (
        <MessageGroup
          key={group[0].message_id}
          channelId={channelId}
          messages={group}
          messagesById={messagesById}
          usersById={usersById}
          onMessageReferenceSelect={onMessageReferenceSelect}
        />
      ))}
    </ol>
  )
})
