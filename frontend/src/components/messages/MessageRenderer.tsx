import type { TicketMessageVersion, TicketMessageView } from "@/types/ticket"
import type { UserSummary } from "@/types/user"

import { DiscordMessageContent } from "./DiscordMessageContent"
import { memo, useMemo, useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { useTranslation } from "react-i18next"
import { formatMessageTimestamp } from "@/utils/timeUtils"
import { ticketService } from "@/services/ticketService"

import clsx from "clsx"

import styles from "./MessageRenderer.module.css"

type MessageRendererProps = {
  ticketId: number
  messages: TicketMessageView[]
  usersById: Record<string, UserSummary>
}

export const MessageRenderer = memo(function MessageRenderer({
  ticketId,
  messages,
  usersById
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
        <Message
          key={message.message_id}
          ticketId={ticketId}
          message={message}
          usersById={usersById}
          referencedMessage={
            message.message_reference_id
              ? (messagesById.get(message.message_reference_id) ?? null)
              : null
          }
          grouped={isGroupedMessage(messages[index - 1], message)}
        />
      ))}
    </ol>
  )
})

type MessageProps = {
  ticketId: number
  message: TicketMessageView
  grouped: boolean
  usersById: Record<string, UserSummary>
  referencedMessage: TicketMessageView | null
}

function Message({
  ticketId,
  message,
  grouped,
  usersById,
  referencedMessage
}: MessageProps) {
  const { t } = useTranslation()
  const [versionsOpen, setVersionsOpen] = useState(false)
  const versionsQuery = useQuery({
    queryKey: ["ticket-message-versions", ticketId, message.message_id],
    queryFn: () => ticketService.messageVersions(ticketId, message.message_id),
    enabled: versionsOpen
  })
  const isDeleted = message.is_deleted
  const content = message.content?.trim()
  const hasContent = Boolean(content)

  return (
    <li
      className={clsx(
        styles.message,
        grouped && styles.grouped,
        isDeleted && styles.deleted
      )}
    >
      {!grouped ? (
        <img className={styles.avatar} src={message.author.avatar_url} alt="" />
      ) : null}
      <div className={styles.body}>
        {!grouped ? (
          <div className={styles.meta}>
            <strong>{message.author.display_name}</strong>
            <time dateTime={message.created_at}>
              {formatMessageTimestamp(message.created_at)}
            </time>
          </div>
        ) : null}
        {message.message_reference_id ? (
          <ReplyPreview message={referencedMessage} />
        ) : null}
        <div className={styles.content}>
          {message.is_edited ? (
            <button
              className={styles.edited}
              type="button"
              aria-expanded={versionsOpen}
              onClick={() => setVersionsOpen((open) => !open)}
            >
              {t("messages.edited")}
            </button>
          ) : null}
          {hasContent ? (
            <DiscordMessageContent
              content={content ?? ""}
              usersById={usersById}
            />
          ) : isDeleted ? (
            <span className={styles.deletedPlaceholder}>
              {t("messages.deleted")}
            </span>
          ) : (
            <span className={styles.noTextContent}>
              {t("messages.noTextContent")}
            </span>
          )}
        </div>
        {versionsOpen ? (
          <MessageVersions
            versions={versionsQuery.data?.versions ?? []}
            loading={versionsQuery.isLoading}
            error={versionsQuery.isError}
          />
        ) : null}
        {message.sticker_id ? (
          <img
            className={styles.sticker}
            src={`https://media.discordapp.net/stickers/${message.sticker_id}.png`}
            alt={t("messages.sticker", { id: message.sticker_id })}
          />
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

function ReplyPreview({ message }: { message: TicketMessageView | null }) {
  const { t } = useTranslation()
  const preview = message?.content?.trim()
  return (
    <div className={styles.reply}>
      <span className={styles.replySpine} aria-hidden="true" />
      {message ? (
        <>
          <strong>{message.author.display_name}</strong>
          <span>{preview || t("messages.noTextContent")}</span>
        </>
      ) : (
        <span className={styles.replyUnavailable}>
          {t("messages.unavailable")}
        </span>
      )}
    </div>
  )
}

function MessageVersions({
  versions,
  loading,
  error
}: {
  versions: TicketMessageVersion[]
  loading: boolean
  error: boolean
}) {
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

function isGroupedMessage(
  previous: TicketMessageView | undefined,
  message: TicketMessageView
): boolean {
  if (
    !previous ||
    previous.author_id !== message.author_id ||
    previous.is_deleted ||
    message.is_deleted
  )
    return false
  return (
    new Date(message.created_at).getTime() -
      new Date(previous.created_at).getTime() <=
    5 * 60 * 1000
  )
}
