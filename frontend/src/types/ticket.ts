import type { UserSummary } from "./user"

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

export type TicketMessage = {
  message_id: string
  author_id: string
  message_reference_id: string | null
  content: string | null
  sticker_id: string | null
  is_edited: boolean
  is_deleted: boolean
  deleted_by_id: string | null
  created_at: string
  updated_at: string
}

export type TicketMessageView = TicketMessage & {
  author: UserSummary
  deleted_by: UserSummary | null
}

export type TicketListQuery = {
  search: string
  status: TicketStatus
  limit?: number
  cursor?: string
}

export type TicketMessagesQuery = {
  limit?: number
  cursor?: string
}

export type TicketPage = {
  tickets: Ticket[]
  next_cursor: string | null
}

export type TicketMessagesPage = {
  ticket: Ticket
  messages: TicketMessage[]
  next_cursor: string | null
}
