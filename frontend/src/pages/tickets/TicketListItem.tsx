import type { Ticket, TicketMessageView } from "@/types/ticket"
import type { UserSummary } from "@/types/user"

import { TicketDetails } from "./TicketDetails"
import { TicketMessages } from "./TicketMessages"
import { FiChevronRight } from "react-icons/fi"
import { formatSimpleTimestamp } from "@/utils/timeUtils"
import { formatTicketNumber } from "@/utils/ticketUtils"
import { useTranslation } from "react-i18next"

import styles from "./TicketListItem.module.css"

type TicketListItemProps = {
  ticket: Ticket
  initiator: UserSummary
  closedBy: UserSummary | null
  expanded: boolean
  messagesRequested: boolean
  messages: TicketMessageView[]
  usersById: Record<string, UserSummary>
  messagesLoading: boolean
  messagesError: string | null
  hasMoreMessages: boolean
  loadingMoreMessages: boolean
  onToggle: () => void
  onLoadMessages: () => void
  onLoadMoreMessages: () => void
  onRetryMessages: () => void
}

export function TicketListItem({
  ticket,
  initiator,
  closedBy,
  expanded,
  messagesRequested,
  messages,
  usersById,
  messagesLoading,
  messagesError,
  hasMoreMessages,
  loadingMoreMessages,
  onToggle,
  onLoadMessages,
  onLoadMoreMessages,
  onRetryMessages
}: TicketListItemProps) {
  const { t } = useTranslation()

  return (
    <li className={styles.entry}>
      <button
        className={styles.summary}
        type="button"
        aria-expanded={expanded}
        onClick={onToggle}
      >
        <FiChevronRight
          className={expanded ? styles.expandedIcon : undefined}
          aria-hidden="true"
        />
        <img
          className={styles.avatar}
          src={initiator.avatar_url}
          alt=""
          draggable={false}
        />
        <span className={styles.ticketTitle}>
          <strong>{formatTicketNumber(ticket.id)}</strong>
          <span>{ticket.title}</span>
        </span>
        <span className={styles.user}>{initiator.username}</span>
        <span className={[styles.status, styles[ticket.status]].join(" ")}>
          {t(`tickets.status.${ticket.status}`)}
        </span>
        <time dateTime={ticket.updated_at}>
          {formatSimpleTimestamp(ticket.updated_at)}
        </time>
      </button>

      {expanded ? (
        <div className={styles.expanded}>
          <TicketDetails
            ticket={ticket}
            initiator={initiator}
            closedBy={closedBy}
          />
          <div className={styles.transcript}>
            <TicketMessages
              ticketId={ticket.id}
              expanded={messagesRequested}
              loading={messagesLoading}
              error={messagesError}
              hasMore={hasMoreMessages}
              loadingMore={loadingMoreMessages}
              messages={messagesRequested ? messages : []}
              usersById={usersById}
              onLoad={onLoadMessages}
              onLoadMore={onLoadMoreMessages}
              onRetry={onRetryMessages}
            />
          </div>
        </div>
      ) : null}
    </li>
  )
}
