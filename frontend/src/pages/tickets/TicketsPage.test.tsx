import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import type { ReactNode } from "react"
import { beforeEach, describe, expect, it, vi } from "vitest"
import { ticketService } from "@/services/ticketService"
import { messageService } from "@/services/messageService"
import { guildDirectoryService } from "@/services/guildDirectoryService"
import { userService } from "@/services/userService"
import { useTicketsStore } from "@/stores/useTicketsStore"
import { useUsersStore } from "@/stores/useUsersStore"
import { useGuildDirectoryStore } from "@/stores/useGuildDirectoryStore"
import type { Ticket } from "@/types/ticket"
import { TicketsPage } from "./TicketsPage"
import "@/services/i18n"

vi.mock("@/components/layout/DashboardLayout", () => ({
  DashboardLayout: ({
    children,
    title
  }: {
    children: ReactNode
    title: string
  }) => (
    <div>
      <h1>{title}</h1>
      {children}
    </div>
  )
}))

vi.mock("@/services/ticketService", () => ({
  ticketService: {
    list: vi.fn()
  }
}))

vi.mock("@/services/messageService", () => ({
  messageService: {
    list: vi.fn(),
    versions: vi.fn(),
    lottieSticker: vi.fn()
  }
}))

vi.mock("@/services/guildDirectoryService", () => ({
  guildDirectoryService: { get: vi.fn() }
}))

vi.mock("@/services/userService", () => ({
  userService: {
    query: vi.fn()
  }
}))

describe("TicketsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useTicketsStore.getState().reset()
    useUsersStore.getState().reset()
    useGuildDirectoryStore.getState().reset()
    vi.mocked(ticketService.list).mockResolvedValue({
      tickets: [ticket],
      next_cursor: null
    })
    vi.mocked(messageService.list).mockResolvedValue({
      channel_id: "456",
      messages: [],
      has_more_before: false,
      has_more_after: false
    })
    vi.mocked(userService.query).mockResolvedValue([user])
    vi.mocked(guildDirectoryService.get).mockResolvedValue({
      channels: [],
      roles: []
    })
  })

  it("expands a ticket without loading its messages", async () => {
    renderPage()

    fireEvent.click(await screen.findByRole("button", { name: /need help/i }))

    expect(screen.getByText("The thing exploded")).toBeInTheDocument()
    expect(screen.getByText("456")).toBeInTheDocument()
    expect(messageService.list).not.toHaveBeenCalled()
  })

  it("loads messages only after the transcript button is clicked", async () => {
    vi.mocked(messageService.list).mockImplementation(
      () => new Promise(() => {})
    )
    renderPage()

    fireEvent.click(await screen.findByRole("button", { name: /need help/i }))
    fireEvent.click(
      screen.getByRole("button", { name: /ler mensagens|read messages/i })
    )

    await waitFor(() =>
      expect(messageService.list).toHaveBeenCalledWith(
        "456",
        expect.objectContaining({ limit: 50 })
      )
    )
    expect(
      await screen.findByRole("status", {
        name: /carregando mensagens|loading messages/i
      })
    ).toBeInTheDocument()
  })

  it("prepends older channel pages using the oldest visible message as before", async () => {
    vi.mocked(messageService.list).mockImplementation((_channelId, query) =>
      Promise.resolve(
        query?.before
          ? {
              channel_id: "456",
              messages: [rawMessage("100", "Older")],
              has_more_before: false,
              has_more_after: true
            }
          : {
              channel_id: "456",
              messages: [rawMessage("200", "Newest")],
              has_more_before: true,
              has_more_after: false
            }
      )
    )
    renderPage()

    fireEvent.click(await screen.findByRole("button", { name: /need help/i }))
    fireEvent.click(
      screen.getByRole("button", { name: /ler mensagens|read messages/i })
    )
    await screen.findByText("Newest")
    fireEvent.click(
      screen.getByRole("button", {
        name: /carregar mensagens anteriores|load older messages/i
      })
    )

    await screen.findByText("Older")
    expect(messageService.list).toHaveBeenLastCalledWith(
      "456",
      expect.objectContaining({ before: "200" })
    )
    expect(
      screen.getAllByText(/Older|Newest/).map((node) => node.textContent)
    ).toEqual(["Older", "Newest"])
  })
})

function renderPage() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false }
    }
  })

  return render(
    <QueryClientProvider client={queryClient}>
      <TicketsPage />
    </QueryClientProvider>
  )
}

const user = {
  id: "42",
  username: "myuu",
  global_name: "Myuu",
  display_name: "Myuu",
  avatar_hash: null,
  avatar_url: "https://cdn.discordapp.com/embed/avatars/0.png"
}

const ticket: Ticket = {
  id: 7,
  title: "Need help",
  description: "The thing exploded",
  guild_id: "123",
  channel_id: "456",
  initiator_id: "42",
  status: "open",
  close_reason: null,
  closed_by_id: null,
  merged_into: null,
  created_at: "2023-11-14T22:13:20Z",
  updated_at: "2023-11-14T22:13:20Z"
}

function rawMessage(messageId: string, content: string) {
  return {
    message_id: messageId,
    author_id: "42",
    message_reference_id: null,
    content,
    sticker_id: null,
    is_edited: false,
    revision_count: 1,
    is_deleted: false,
    deleted_by_id: null,
    created_at: "2023-11-14T22:13:20Z",
    updated_at: "2023-11-14T22:13:20Z"
  }
}
