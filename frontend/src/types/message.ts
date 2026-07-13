import type { UserSummary } from "./user"

export type Message = {
  message_id: string
  author_id: string
  message_reference_id: string | null
  content: string | null
  sticker_id: string | null
  is_edited: boolean
  revision_count: number
  is_deleted: boolean
  deleted_by_id: string | null
  created_at: string
  updated_at: string
}

export type MessageVersion = {
  message_id: string
  author_id: string
  message_reference_id: string | null
  content: string | null
  sticker_id: string | null
  created_at: string
}

export type MessageView = Message & {
  author: UserSummary
  deleted_by: UserSummary | null
}

type ChannelMessageAnchor =
  | { before: string; after?: never; around?: never }
  | { before?: never; after: string; around?: never }
  | { before?: never; after?: never; around: string }
  | { before?: never; after?: never; around?: never }

export type ChannelMessagesQuery = { limit?: number } & ChannelMessageAnchor

export type ChannelMessagesPage = {
  channel_id: string
  messages: Message[]
  has_more_before: boolean
  has_more_after: boolean
}

export type MessageVersionsResponse = {
  versions: MessageVersion[]
}
