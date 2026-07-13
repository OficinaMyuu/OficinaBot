import type { Ticket } from "@/types/ticket"

export function formatTicketNumber(id: number): string {
  return `#${String(id).padStart(2, "0")}`
}

export function ticketUserIds(tickets: Ticket[]): string[] {
  return tickets.flatMap((ticket) =>
    [ticket.initiator_id, ticket.closed_by_id].filter(Boolean)
  ) as string[]
}
