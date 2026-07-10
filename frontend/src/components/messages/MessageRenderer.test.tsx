import { render, screen } from "@testing-library/react"
import { describe, expect, it } from "vitest"
import { MessageRenderer } from "./MessageRenderer"
import "@/services/i18n"
import type { TicketMessageView } from "@/types/ticket"

describe("MessageRenderer", () => {
  it("renders edited, referenced, and sticker message metadata", () => {
    render(
      <MessageRenderer
        messages={[
          message({
            is_edited: true,
            message_reference_id: "100",
            sticker_id: "200"
          })
        ]}
      />
    )

    expect(screen.getByText("Myuu")).toBeInTheDocument()
    expect(screen.getByText("Hello there")).toBeInTheDocument()
    expect(screen.getByText(/edited|editada/i)).toBeInTheDocument()
    expect(screen.getByText(/#100/i)).toBeInTheDocument()
    expect(screen.getByText(/200/i)).toBeInTheDocument()
  })

  it("renders deleted message state without dropping audit context", () => {
    render(
      <MessageRenderer
        messages={[
          message({
            is_deleted: true,
            deleted_by_id: "99",
            deleted_by: {
              id: "99",
              username: "staff",
              global_name: null,
              display_name: "Staff",
              avatar_hash: null,
              avatar_url: "/staff.png"
            }
          })
        ]}
      />
    )

    expect(
      screen.getByText(/message deleted|mensagem apagada/i)
    ).toBeInTheDocument()
    expect(screen.getByText(/staff/i)).toBeInTheDocument()
  })
})

function message(
  overrides: Partial<TicketMessageView> = {}
): TicketMessageView {
  return {
    message_id: "101",
    author_id: "42",
    author: {
      id: "42",
      username: "myuu",
      global_name: "Myuu",
      display_name: "Myuu",
      avatar_hash: null,
      avatar_url: "/avatar.png"
    },
    message_reference_id: null,
    content: "Hello there",
    sticker_id: null,
    is_edited: false,
    is_deleted: false,
    deleted_by_id: null,
    deleted_by: null,
    created_at: "2023-11-14T22:13:20Z",
    updated_at: "2023-11-14T22:13:20Z",
    ...overrides
  }
}
