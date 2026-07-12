import type { Ticket, TicketMessageView } from "@/types/ticket"
import type { UserSummary } from "@/types/user"
import { fallbackUser } from "@/stores/useUsersStore"
import { TicketListItem } from "./TicketListItem"
import styles from "./TicketList.module.css"

type TicketListProps = {
  tickets: Ticket[]
  usersById: Record<string, UserSummary>
  expandedTicketId: number | null
  messagesTicketId: number | null
  messages: TicketMessageView[]
  messagesLoading: boolean
  messagesError: string | null
  hasMoreMessages: boolean
  loadingMoreMessages: boolean
  listLabel: string
  onToggleTicket: (ticketId: number) => void
  onLoadMessages: (ticketId: number) => void
  onLoadMoreMessages: () => void
  onRetryMessages: () => void
}

export function TicketList({
  tickets,
  usersById,
  expandedTicketId,
  messagesTicketId,
  messages,
  messagesLoading,
  messagesError,
  hasMoreMessages,
  loadingMoreMessages,
  listLabel,
  onToggleTicket,
  onLoadMessages,
  onLoadMoreMessages,
  onRetryMessages
}: TicketListProps) {
  return (
    <ol className={styles.entries} aria-label={listLabel}>
      {tickets.map((ticket) => {
        const expanded = expandedTicketId === ticket.id
        const messagesRequested = messagesTicketId === ticket.id
        const initiator =
          usersById[ticket.initiator_id] ?? fallbackUser(ticket.initiator_id)
        const closedBy = ticket.closed_by_id
          ? (usersById[ticket.closed_by_id] ?? fallbackUser(ticket.closed_by_id))
          : null

        return (
          <TicketListItem
            key={ticket.id}
            ticket={ticket}
            initiator={initiator}
            closedBy={closedBy}
            expanded={expanded}
            messagesRequested={messagesRequested}
            messages={messages}
            messagesLoading={messagesLoading}
            messagesError={messagesError}
            hasMoreMessages={hasMoreMessages}
            loadingMoreMessages={loadingMoreMessages}
            onToggle={() => onToggleTicket(ticket.id)}
            onLoadMessages={() => onLoadMessages(ticket.id)}
            onLoadMoreMessages={onLoadMoreMessages}
            onRetryMessages={onRetryMessages}
          />
        )
      })}
    </ol>
  )
}
