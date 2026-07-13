import { describe, expect, it } from "vitest"
import type { Ticket } from "@/types/ticket"
import { formatTicketNumber, ticketUserIds } from "./ticketUtils"

describe("ticketUtils", () => {
  it("formats ticket numbers with a minimum two-digit prefix", () => {
    expect(formatTicketNumber(7)).toBe("#07")
    expect(formatTicketNumber(128)).toBe("#128")
  })

  it("collects only present ticket user IDs", () => {
    expect(ticketUserIds([ticket])).toEqual(["42", "99"])
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
