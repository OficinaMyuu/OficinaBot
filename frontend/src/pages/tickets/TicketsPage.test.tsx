import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import type { ReactNode } from "react"
import { beforeEach, describe, expect, it, vi } from "vitest"
import { ticketService } from "@/services/ticketService"
import { userService } from "@/services/userService"
import { useTicketsStore } from "@/stores/useTicketsStore"
import { useUsersStore } from "@/stores/useUsersStore"
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
    list: vi.fn(),
    messages: vi.fn()
  }
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
    vi.mocked(ticketService.list).mockResolvedValue({
      tickets: [ticket],
      next_cursor: null
    })
    vi.mocked(ticketService.messages).mockResolvedValue({
      ticket,
      messages: [],
      next_cursor: null
    })
    vi.mocked(userService.query).mockResolvedValue([user])
  })

  it("expands a ticket without loading its messages", async () => {
    renderPage()

    fireEvent.click(await screen.findByRole("button", { name: /need help/i }))

    expect(screen.getByText("The thing exploded")).toBeInTheDocument()
    expect(screen.getByText("456")).toBeInTheDocument()
    expect(ticketService.messages).not.toHaveBeenCalled()
  })

  it("loads messages only after the transcript button is clicked", async () => {
    vi.mocked(ticketService.messages).mockImplementation(
      () => new Promise(() => {})
    )
    renderPage()

    fireEvent.click(await screen.findByRole("button", { name: /need help/i }))
    fireEvent.click(
      screen.getByRole("button", { name: /ler mensagens|read messages/i })
    )

    await waitFor(() =>
      expect(ticketService.messages).toHaveBeenCalledWith(
        7,
        expect.objectContaining({ limit: 50 })
      )
    )
    expect(
      await screen.findByRole("status", {
        name: /carregando mensagens|loading messages/i
      })
    ).toBeInTheDocument()
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
