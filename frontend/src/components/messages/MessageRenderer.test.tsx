import { render, screen } from "@testing-library/react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import type { ReactNode } from "react"
import { describe, expect, it } from "vitest"
import { MessageRenderer } from "./MessageRenderer"
import "@/services/i18n"
import type { TicketMessageView } from "@/types/ticket"

describe("MessageRenderer", () => {
  it("renders edited, referenced, and sticker message metadata", () => {
    renderMessageRenderer(
      <MessageRenderer
        ticketId={7}
        usersById={{}}
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
    expect(screen.getByText(/message could not be loaded|mensagem não pôde ser carregada/i)).toBeInTheDocument()
    expect(screen.getByRole("img", { name: /sticker 200/i })).toBeInTheDocument()
  })

  it("renders deleted message state without dropping audit context", () => {
    renderMessageRenderer(
      <MessageRenderer
        ticketId={7}
        usersById={{}}
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

    expect(screen.getByText("Hello there")).toBeInTheDocument()
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
    revision_count: 1,
    is_deleted: false,
    deleted_by_id: null,
    deleted_by: null,
    created_at: "2023-11-14T22:13:20Z",
    updated_at: "2023-11-14T22:13:20Z",
    ...overrides
  }
}

function renderMessageRenderer(node: ReactNode) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } }
  })
  return render(
    <QueryClientProvider client={client}>{node}</QueryClientProvider>
  )
}
