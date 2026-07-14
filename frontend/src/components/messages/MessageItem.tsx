import type { MessageView } from "@/types/message"
import type { UserSummary } from "@/types/user"

import { useState } from "react"
import { DiscordMessageContent } from "./DiscordMessageContent"
import { MessageVersions } from "./MessageVersions"
import { ReplyPreview } from "./ReplyPreview"
import { Sticker } from "./Sticker"
import { useQuery } from "@tanstack/react-query"
import { useTranslation } from "react-i18next"
import { messageService } from "@/services/messageService"
import { formatMessageTimestamp } from "@/utils/timeUtils"

import clsx from "clsx"

import styles from "./MessageItem.module.css"

type MessageItemProps = {
  channelId: string
  message: MessageView
  grouped: boolean
  usersById: Record<string, UserSummary>
  referencedMessage: MessageView | null
  onMessageReferenceSelect?: (messageId: string) => void
}

export function MessageItem({
  channelId,
  message,
  grouped,
  usersById,
  referencedMessage,
  onMessageReferenceSelect
}: MessageItemProps) {
  const { t } = useTranslation()
  const [versionsOpen, setVersionsOpen] = useState(false)
  const versionsQuery = useQuery({
    queryKey: ["message-versions", channelId, message.message_id],
    queryFn: () => messageService.versions(channelId, message.message_id),
    enabled: versionsOpen
  })
  const content = message.content?.trim()
  const hasContent = Boolean(content)
  const isReply = message.message_reference_id !== null

  return (
    <li
      id={`message-${message.message_id}`}
      data-message-id={message.message_id}
      tabIndex={-1}
      className={clsx(
        styles.message,
        grouped && styles.grouped,
        isReply && styles.replying,
        message.is_deleted && styles.deleted
      )}
    >
      {!grouped ? (
        <img className={styles.avatar} src={message.author.avatar_url} alt="" />
      ) : null}
      <div className={styles.body}>
        {isReply ? (
          <ReplyPreview
            message={referencedMessage}
            referencedMessageId={message.message_reference_id}
            onSelect={onMessageReferenceSelect}
          />
        ) : null}
        {!grouped ? (
          <div className={styles.meta}>
            <strong className={styles.displayName}>
              {message.author.display_name}
            </strong>
            {message.author.is_bot ? (
              <span className={styles.botBadge}>BOT</span>
            ) : null}
            <time className={styles.timestamp} dateTime={message.created_at}>
              {formatMessageTimestamp(message.created_at)}
            </time>
          </div>
        ) : null}
        <div
          className={clsx(
            styles.content,
            message.is_edited && styles.editedContent
          )}
        >
          {hasContent ? (
            <DiscordMessageContent
              content={content ?? ""}
              usersById={usersById}
            />
          ) : message.is_deleted ? (
            <span className={styles.deletedPlaceholder}>
              {t("messages.deleted")}
            </span>
          ) : !message.sticker_id ? (
            <span className={styles.noTextContent}>
              {t("messages.noTextContent")}
            </span>
          ) : null}
          {message.is_edited ? (
            <button
              className={styles.edited}
              type="button"
              aria-expanded={versionsOpen}
              onClick={() => setVersionsOpen((open) => !open)}
            >
              ({t("messages.edited")})
            </button>
          ) : null}
        </div>
        {versionsOpen ? (
          <MessageVersions
            versions={versionsQuery.data?.versions ?? []}
            loading={versionsQuery.isLoading}
            error={versionsQuery.isError}
          />
        ) : null}
        {message.sticker_id ? (
          <Sticker key={message.sticker_id} stickerId={message.sticker_id} />
        ) : null}
        {message.deleted_by ? (
          <div className={styles.audit}>
            {t("messages.deletedBy", { name: message.deleted_by.display_name })}
          </div>
        ) : null}
      </div>
    </li>
  )
}
