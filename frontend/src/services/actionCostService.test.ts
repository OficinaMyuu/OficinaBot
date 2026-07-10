import { afterEach, describe, expect, it, vi } from "vitest"
import { actionCostService } from "./actionCostService"

describe("actionCostService", () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it("lists action costs from the protected economy endpoint", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ items: [] }))
    vi.stubGlobal("fetch", fetchMock)

    await actionCostService.list()

    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/economy/action-costs",
      expect.objectContaining({ credentials: "include", method: "GET" })
    )
  })

  it("updates an action cost", async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({}))
    vi.stubGlobal("fetch", fetchMock)

    await actionCostService.update("GROUP", { price: 0 })

    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/economy/action-costs/GROUP",
      expect.objectContaining({
        body: JSON.stringify({ price: 0 }),
        credentials: "include",
        method: "PATCH"
      })
    )
  })
})

function jsonResponse(body: unknown): Response {
  return new Response(JSON.stringify(body), {
    headers: { "Content-Type": "application/json" },
    status: 200
  })
}
