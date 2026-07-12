import type { Ticket, TicketMessage, TicketMessageView } from "@/types/ticket"
import type { UserSummary } from "@/types/user"

export function formatTicketNumber(id: number): string {
  return `#${String(id).padStart(2, "0")}`
}

export function ticketUserIds(tickets: Ticket[]): string[] {
  return tickets.flatMap((ticket) =>
    [ticket.initiator_id, ticket.closed_by_id].filter(Boolean)
  ) as string[]
}

export function messageUserIds(messages: TicketMessage[]): string[] {
  return messages.flatMap((message) =>
    [message.author_id, message.deleted_by_id].filter(Boolean)
  ) as string[]
}

export function toTicketMessageViews(
  messages: TicketMessage[],
  usersById: Record<string, UserSummary>,
  getFallbackUser: (userId: string) => UserSummary
): TicketMessageView[] {
  return messages.map((message) => ({
    ...message,
    author: usersById[message.author_id] ?? getFallbackUser(message.author_id),
    deleted_by: message.deleted_by_id
      ? (usersById[message.deleted_by_id] ??
        getFallbackUser(message.deleted_by_id))
      : null
  }))
}
