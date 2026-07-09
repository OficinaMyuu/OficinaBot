export type TicketStatus = 'all' | 'open' | 'closed'

export type TicketUser = {
  id: string
  username: string | null
  global_name: string | null
  display_name: string
  avatar_url: string
}

export type Ticket = {
  id: number
  title: string
  description: string
  guild_id: string
  channel_id: string
  initiator: TicketUser
  status: Exclude<TicketStatus, 'all'>
  close_reason: string | null
  closed_by: TicketUser | null
  merged_into: number | null
  created_at: number
  updated_at: number
}

export type TicketMessage = {
  message_id: string
  author: TicketUser
  message_reference_id: string | null
  content: string | null
  sticker_id: string | null
  is_edited: boolean
  is_deleted: boolean
  deleted_by: TicketUser | null
  created_at: number
  updated_at: number
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
