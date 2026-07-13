import type { Ticket } from "@/types/ticket"
import type { UserSummary } from "@/types/user"
import { fallbackUser } from "@/stores/useUsersStore"
import { TicketListItem } from "./TicketListItem"
import styles from "./TicketList.module.css"

type TicketListProps = {
  tickets: Ticket[]
  usersById: Record<string, UserSummary>
  expandedTicketId: number | null
  listLabel: string
  onToggleTicket: (ticketId: number) => void
}

export function TicketList({
  tickets,
  usersById,
  expandedTicketId,
  listLabel,
  onToggleTicket
}: TicketListProps) {
  return (
    <ol className={styles.entries} aria-label={listLabel}>
      {tickets.map((ticket) => {
        const expanded = expandedTicketId === ticket.id
        const initiator =
          usersById[ticket.initiator_id] ?? fallbackUser(ticket.initiator_id)
        const closedBy = ticket.closed_by_id
          ? (usersById[ticket.closed_by_id] ??
            fallbackUser(ticket.closed_by_id))
          : null

        return (
          <TicketListItem
            key={ticket.id}
            ticket={ticket}
            initiator={initiator}
            closedBy={closedBy}
            expanded={expanded}
            onToggle={() => onToggleTicket(ticket.id)}
          />
        )
      })}
    </ol>
  )
}
