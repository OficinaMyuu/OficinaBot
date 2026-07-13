import { describe, expect, it } from "vitest"
import type { Ticket, TicketMessage } from "@/types/ticket"
import type { UserSummary } from "@/types/user"
import {
  formatTicketNumber,
  messageUserIds,
  ticketUserIds,
  toTicketMessageViews
} from "./ticketUtils"

describe("ticketUtils", () => {
  it("formats ticket numbers with a minimum two-digit prefix", () => {
    expect(formatTicketNumber(7)).toBe("#07")
    expect(formatTicketNumber(128)).toBe("#128")
  })

  it("collects only present ticket and message user IDs", () => {
    expect(ticketUserIds([ticket])).toEqual(["42", "99"])
    expect(messageUserIds([message])).toEqual(["42"])
  })

  it("resolves known and fallback message users", () => {
    const fallbackUser = createUser("fallback")
    const messages = toTicketMessageViews(
      [{ ...message, deleted_by_id: "99" }],
      { "42": createUser("author") },
      () => fallbackUser
    )

    expect(messages[0].author.username).toBe("author")
    expect(messages[0].deleted_by).toBe(fallbackUser)
  })
})

const ticket: Ticket = {
  id: 7,
  title: "Need help",
  description: "The thing exploded",
  guild_id: "123",
  channel_id: "456",
  initiator_id: "42",
  status: "open",
  close_reason: null,
  closed_by_id: "99",
  merged_into: null,
  created_at: "2023-11-14T22:13:20Z",
  updated_at: "2023-11-14T22:13:20Z"
}

const message: TicketMessage = {
  message_id: "1",
  author_id: "42",
  message_reference_id: null,
  content: "Hello",
  sticker_id: null,
  is_edited: false,
  revision_count: 1,
  is_deleted: false,
  deleted_by_id: null,
  created_at: "2023-11-14T22:13:20Z",
  updated_at: "2023-11-14T22:13:20Z"
}

function createUser(username: string): UserSummary {
  return {
    id: username,
    username,
    global_name: username,
    display_name: username,
    avatar_hash: null,
    avatar_url: "https://cdn.discordapp.com/embed/avatars/0.png"
  }
}
