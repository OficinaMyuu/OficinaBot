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

  it("edits and saves a zero-cost action", async () => {
    renderPage()

    fireEvent.click(await screen.findByTitle(/editar custo|edit cost/i))
    fireEvent.change(screen.getByLabelText(/criar grupo|create group/i), {
      target: { value: "0" }
    })
    fireEvent.click(screen.getByRole("button", { name: /salvar custo|save cost/i }))

    await waitFor(() =>
      expect(actionCostService.update).toHaveBeenCalledWith("GROUP", {
        price: 0
      })
    )
    expect(await screen.findByText(/atualizado|updated/i)).toBeInTheDocument()
  })

  it("rejects negative values before calling the API", async () => {
    renderPage()

    fireEvent.click(await screen.findByTitle(/editar custo|edit cost/i))
    fireEvent.change(screen.getByLabelText(/criar grupo|create group/i), {
      target: { value: "-1" }
    })
    fireEvent.click(screen.getByRole("button", { name: /salvar custo|save cost/i }))

    expect(actionCostService.update).not.toHaveBeenCalled()
    expect(await screen.findByText(/maior que zero|zero or greater/i)).toBeInTheDocument()
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
