import type { Ticket } from "@/types/ticket"
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
  onToggle: () => void
}

export function TicketListItem({
  ticket,
  initiator,
  closedBy,
  expanded,
  onToggle
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
            <TicketMessages channelId={ticket.channel_id} />
          </div>
        </div>
      ) : null}
    </li>
  )
}
