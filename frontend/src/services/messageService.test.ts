import { afterEach, describe, expect, it, vi } from "vitest"
import { messageService } from "./messageService"

describe("messageService", () => {
  afterEach(() => vi.unstubAllGlobals())

  it("loads channel messages using directional message anchors", async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      new Response(
        JSON.stringify({
          channel_id: "456",
          messages: [],
          has_more_before: false,
          has_more_after: false
        }),
        { headers: { "Content-Type": "application/json" }, status: 200 }
      )
    )
    vi.stubGlobal("fetch", fetchMock)

    await messageService.list("456", { limit: 50, around: "101" })

    expect(fetchMock).toHaveBeenCalledWith(
      "http://localhost:8080/channels/456/messages?limit=50&around=101",
      expect.objectContaining({ credentials: "include", method: "GET" })
    )
  })
})
