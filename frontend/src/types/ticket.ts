export type TicketStatus = "all" | "open" | "closed"

export type Ticket = {
  id: number
  title: string
  description: string
  guild_id: string
  channel_id: string
  initiator_id: string
  status: Exclude<TicketStatus, "all">
  close_reason: string | null
  closed_by_id: string | null
  merged_into: number | null
  created_at: string
  updated_at: string
}

export type TicketListQuery = {
  search: string
  status: TicketStatus
  limit?: number
  cursor?: string
}

export type TicketPage = {
  tickets: Ticket[]
  next_cursor: string | null
}
