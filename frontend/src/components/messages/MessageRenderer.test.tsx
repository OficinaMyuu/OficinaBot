import { fireEvent, render, screen } from "@testing-library/react"
import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import type { ReactNode } from "react"
import { describe, expect, it, vi } from "vitest"
import { MessageRenderer } from "./MessageRenderer"
import "@/services/i18n"
import type { MessageView } from "@/types/message"

describe("MessageRenderer", () => {
  it("renders edited, referenced, and sticker message metadata", () => {
    renderMessageRenderer(
      <MessageRenderer
        channelId="456"
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
    expect(
      screen.getByText(
        /message could not be loaded|mensagem não pôde ser carregada/i
      )
    ).toBeInTheDocument()
    expect(
      screen.getByRole("img", { name: /sticker 200/i })
    ).toBeInTheDocument()
  })

  it("renders deleted message state without dropping audit context", () => {
    renderMessageRenderer(
      <MessageRenderer
        channelId="456"
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

  it("starts a full author group when a consecutive message is a reply", () => {
    const onSelect = vi.fn()
    const { container } = renderMessageRenderer(
      <MessageRenderer
        channelId="456"
        usersById={{}}
        onMessageReferenceSelect={onSelect}
        messages={[
          message({ message_id: "100", content: "Original" }),
          message({
            message_id: "101",
            created_at: "2023-11-14T22:14:20Z"
          }),
          message({
            message_id: "102",
            message_reference_id: "100",
            created_at: "2023-11-14T22:15:20Z"
          })
        ]}
      />
    )

    expect(container.querySelectorAll('img[src="/avatar.png"]')).toHaveLength(2)
    expect(
      screen.getByRole("list", {
        name: /message log|historico de mensagens/i
      }).children
    ).toHaveLength(2)
    expect(container.querySelectorAll("[data-message-id]")).toHaveLength(3)
    expect(screen.getAllByText("Myuu")).toHaveLength(3)
    fireEvent.click(screen.getByRole("button", { name: /myuu original/i }))
    expect(onSelect).toHaveBeenCalledWith("100")
  })

  it("describes sticker-only reply references without empty-text copy", () => {
    const onSelect = vi.fn()
    renderMessageRenderer(
      <MessageRenderer
        channelId="456"
        usersById={{}}
        onMessageReferenceSelect={onSelect}
        messages={[
          message({ message_id: "100", content: null, sticker_id: "200" }),
          message({
            message_id: "101",
            message_reference_id: "100",
            created_at: "2023-11-14T22:14:20Z"
          })
        ]}
      />
    )

    const attachment = screen.getByRole("button", {
      name: /click to view attachment|clique para ver anexo/i
    })
    expect(
      screen.queryByText(/^no text content$|^sem texto$/i)
    ).not.toBeInTheDocument()

    fireEvent.click(attachment)
    expect(onSelect).toHaveBeenCalledWith("100")
  })

  it("renders the edited control after the message content", () => {
    renderMessageRenderer(
      <MessageRenderer
        channelId="456"
        usersById={{}}
        messages={[message({ is_edited: true })]}
      />
    )

    const content = screen.getByText("Hello there")
    const edited = screen.getByRole("button", { name: /edited|editada/i })

    expect(
      content.compareDocumentPosition(edited) & Node.DOCUMENT_POSITION_FOLLOWING
    ).toBeTruthy()
  })
})

function message(overrides: Partial<MessageView> = {}): MessageView {
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
