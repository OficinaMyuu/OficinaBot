import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { describe, expect, it, vi } from "vitest"
import { messageService } from "@/services/messageService"
import { Sticker } from "./Sticker"
import "@/services/i18n"

vi.mock("@/services/messageService", () => ({
  messageService: { lottieSticker: vi.fn() }
}))

vi.mock("lottie-web/build/player/lottie_light", () => ({
  default: {
    loadAnimation: vi.fn(() => ({
      destroy: vi.fn(),
      goToAndStop: vi.fn()
    }))
  }
}))

describe("Sticker", () => {
  it("falls back from Discord PNG to the authenticated Lottie asset", async () => {
    vi.mocked(messageService.lottieSticker).mockResolvedValue({
      v: "5.6.2",
      layers: []
    })
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } }
    })
    render(
      <QueryClientProvider client={client}>
        <Sticker stickerId="749054660769218631" />
      </QueryClientProvider>
    )

    fireEvent.error(screen.getByRole("img"))

    await waitFor(() =>
      expect(messageService.lottieSticker).toHaveBeenCalledWith(
        "749054660769218631"
      )
    )
    expect(screen.getByRole("img")).toBeInTheDocument()
  })
})
