import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import type { ReactNode } from "react"
import { beforeEach, describe, expect, it, vi } from "vitest"
import { actionCostService } from "@/services/actionCostService"
import { ActionCostsPage } from "./ActionCostsPage"
import "@/services/i18n"

vi.mock("@/components/layout/DashboardLayout", () => ({
  DashboardLayout: ({ children, title }: { children: ReactNode; title: string }) => (
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
    vi.mocked(actionCostService.list).mockResolvedValue([actionCost])
    vi.mocked(actionCostService.update).mockResolvedValue({
      ...actionCost,
      price: 0,
      updated_by: "42"
    })
  })

  it("inline-edits a cost to zero via Enter key", async () => {
    renderPage()

    const costCell = await screen.findByText("600.000")
    fireEvent.click(costCell)

    const input = screen.getByLabelText("GROUP")
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

    const costCell = await screen.findByText("600.000")
    fireEvent.click(costCell)

    const input = screen.getByLabelText("GROUP")
    fireEvent.change(input, { target: { value: "-1" } })
    fireEvent.keyDown(input, { key: "Enter" })

    expect(actionCostService.update).not.toHaveBeenCalled()
    expect(await screen.findByText(/maior que zero|zero or greater/i)).toBeInTheDocument()
  })

  it("does not render an edit button", async () => {
    renderPage()

    await screen.findByText("600.000")
    expect(screen.queryByTitle(/editar custo|edit cost/i)).not.toBeInTheDocument()
  })

  it("cancels editing on Escape key", async () => {
    renderPage()

    const costCell = await screen.findByText("600.000")
    fireEvent.click(costCell)

    const input = screen.getByLabelText("GROUP")
    fireEvent.change(input, { target: { value: "999" } })
    fireEvent.keyDown(input, { key: "Escape" })

    expect(actionCostService.update).not.toHaveBeenCalled()
    expect(screen.getByText("600.000")).toBeInTheDocument()
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
