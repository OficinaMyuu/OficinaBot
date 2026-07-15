import { QueryClient, QueryClientProvider } from "@tanstack/react-query"
import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest"
import { messageService } from "@/services/messageService"
import { getDiscordStickerUrl } from "@/config/discordUrls"
import { Sticker } from "./Sticker"
import "@/services/i18n"

vi.mock("@/services/messageService", () => ({
  messageService: { lottieSticker: vi.fn() }
}))

const { loadAnimation } = vi.hoisted(() => ({
  loadAnimation: vi.fn(() => ({
    destroy: vi.fn(),
    goToAndStop: vi.fn()
  }))
}))

vi.mock("lottie-web/build/player/lottie_light", () => ({
  default: { loadAnimation }
}))

describe("Sticker", () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.stubGlobal(
      "matchMedia",
      vi.fn(() => ({ matches: false }))
    )
  })

  afterEach(() => vi.unstubAllGlobals())

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

    expect(screen.getByRole("img")).toHaveAttribute(
      "src",
      getDiscordStickerUrl("749054660769218631")
    )
    fireEvent.error(screen.getByRole("img"))

    await waitFor(() =>
      expect(messageService.lottieSticker).toHaveBeenCalledWith(
        "749054660769218631"
      )
    )
    await waitFor(() => expect(loadAnimation).toHaveBeenCalledOnce())
    expect(screen.getByRole("img")).toBeInTheDocument()
  })

  it("initializes cached Lottie data after the fallback container mounts", async () => {
    const stickerId = "749054660769218631"
    const animationData = { v: "5.6.2", layers: [] }
    const client = new QueryClient({
      defaultOptions: { queries: { retry: false } }
    })
    client.setQueryData(["discord-sticker-lottie", stickerId], animationData)
    render(
      <QueryClientProvider client={client}>
        <Sticker stickerId={stickerId} />
      </QueryClientProvider>
    )

    expect(messageService.lottieSticker).not.toHaveBeenCalled()
    fireEvent.error(screen.getByRole("img"))

    await waitFor(() =>
      expect(loadAnimation).toHaveBeenCalledWith(
        expect.objectContaining({
          animationData,
          container: expect.any(HTMLDivElement)
        })
      )
    )
  })
})
