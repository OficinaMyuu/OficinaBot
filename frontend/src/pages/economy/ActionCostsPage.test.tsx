import type { ReactNode } from "react"

import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { beforeEach, describe, expect, it, vi } from "vitest"
import { actionCostService } from "@/services/actionCostService"
import { userService } from "@/services/userService"
import { useUsersStore } from "@/stores/useUsersStore"
import { ActionCostsPage } from "./ActionCostsPage"

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

vi.mock("@/services/actionCostService", () => ({
  actionCostService: {
    list: vi.fn(),
    update: vi.fn()
  }
}))

vi.mock("@/services/userService", () => ({
  userService: {
    query: vi.fn()
  }
}))

const actionCost = {
  item_type: "GROUP" as const,
  price: 600000,
  created_at: "2023-11-14T22:13:20Z",
  updated_at: "2023-11-14T22:13:20Z",
  updated_by: null
}

describe("ActionCostsPage", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    useUsersStore.getState().reset()
    vi.mocked(actionCostService.list).mockResolvedValue([actionCost])
    vi.mocked(userService.query).mockResolvedValue([user])
    vi.mocked(actionCostService.update).mockResolvedValue({
      ...actionCost,
      price: 0,
      updated_by: "42"
    })
  })

  it("inline-edits a cost to zero via Enter key", async () => {
    renderPage()

    const input = await screen.findByLabelText("GROUP")
    fireEvent.click(input)
    fireEvent.change(input, { target: { value: "0" } })
    fireEvent.keyDown(input, { key: "Enter" })

    await waitFor(() =>
      expect(actionCostService.update).toHaveBeenCalledWith("GROUP", {
        price: 0
      })
    )
    expect(await screen.findByText(/salvas|updated/i)).toBeInTheDocument()
  })

  it("rejects negative values before calling the API", async () => {
    renderPage()

    const input = await screen.findByLabelText("GROUP")
    fireEvent.click(input)
    fireEvent.change(input, { target: { value: "-1" } })
    fireEvent.keyDown(input, { key: "Enter" })

    expect(actionCostService.update).not.toHaveBeenCalled()
    expect(
      await screen.findByText(/maior que zero|zero or greater/i)
    ).toBeInTheDocument()
  })

  it("does not render an edit button", async () => {
    renderPage()

    await screen.findByLabelText("GROUP")
    expect(
      screen.queryByTitle(/editar custo|edit cost/i)
    ).not.toBeInTheDocument()
  })

  it("renders a color-role action cost", async () => {
    vi.mocked(actionCostService.list).mockResolvedValue([
      { ...actionCost, item_type: "COLOR_ROLE", price: 75000 }
    ])

    renderPage()

    expect(
      await screen.findByText(/cargo de cor|color role/i)
    ).toBeInTheDocument()
    expect(screen.getByText("/colors")).toBeInTheDocument()
  })

  it("cancels editing on Escape key", async () => {
    renderPage()

    const input = await screen.findByLabelText("GROUP")
    fireEvent.click(input)
    fireEvent.change(input, { target: { value: "999" } })
    fireEvent.keyDown(input, { key: "Escape" })

    expect(actionCostService.update).not.toHaveBeenCalled()
    expect(screen.getByLabelText("GROUP")).toHaveValue("600.000")
  })

  it("shows the last updater avatar with their effective Discord name", async () => {
    vi.mocked(actionCostService.list).mockResolvedValue([
      { ...actionCost, updated_by: "42" }
    ])

    renderPage()

    const avatar = await screen.findByAltText("")
    await waitFor(() => expect(userService.query).toHaveBeenCalledWith(["42"]))

    fireEvent.pointerMove(avatar, { pointerType: "mouse" })

    expect(await screen.findByRole("tooltip")).toHaveTextContent("Myuu")
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
      <ActionCostsPage />
    </QueryClientProvider>
  )
}

const user = {
  id: "42",
  username: "myuu",
  global_name: "Myuu",
  display_name: "Myuu",
  avatar_hash: null,
  avatar_url: "https://cdn.discordapp.com/embed/avatars/0.png",
  is_bot: false
}
