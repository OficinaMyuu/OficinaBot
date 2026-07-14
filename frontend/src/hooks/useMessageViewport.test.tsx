import { act, renderHook } from "@testing-library/react"
import { describe, expect, it, vi } from "vitest"
import type { MessageView } from "@/types/message"
import { useMessageViewport } from "./useMessageViewport"

describe("useMessageViewport", () => {
  it("opens at the bottom and preserves the visible anchor after prepend", () => {
    const onLoadOlder = vi.fn()
    const initialProps: { messages: MessageView[] } = { messages: [] }
    const { result, rerender } = renderHook(
      ({ messages }: { messages: MessageView[] }) =>
        useMessageViewport({
          channelId: "456",
          messages,
          hasMoreBefore: true,
          loadingMore: false,
          onLoadOlder
        }),
      { initialProps }
    )
    const viewport = document.createElement("div")
    let scrollHeight = 500
    Object.defineProperties(viewport, {
      clientHeight: { configurable: true, value: 200 },
      scrollHeight: { configurable: true, get: () => scrollHeight }
    })
    result.current.viewportRef.current = viewport

    rerender({ messages: [message("2"), message("3")] })
    expect(viewport.scrollTop).toBe(500)

    viewport.scrollTop = 40
    scrollHeight = 700
    rerender({ messages: [message("1"), message("2"), message("3")] })
    expect(viewport.scrollTop).toBe(240)

    act(() => {
      result.current.onScroll({ currentTarget: viewport } as never)
    })
    expect(onLoadOlder).not.toHaveBeenCalled()
    viewport.scrollTop = 40
    act(() => {
      result.current.onScroll({ currentTarget: viewport } as never)
    })
    expect(onLoadOlder).toHaveBeenCalledOnce()
  })

  it("scrolls to and focuses a loaded message", () => {
    const { result } = renderHook(() =>
      useMessageViewport({
        channelId: "456",
        messages: [],
        hasMoreBefore: false,
        loadingMore: false,
        onLoadOlder: vi.fn()
      })
    )
    const viewport = document.createElement("div")
    const messageElement = document.createElement("article")
    const scrollIntoView = vi.fn()
    const focus = vi.fn()
    messageElement.dataset.messageId = "100"
    messageElement.scrollIntoView = scrollIntoView
    messageElement.focus = focus
    viewport.append(messageElement)
    result.current.viewportRef.current = viewport

    expect(result.current.scrollToMessage("100")).toBe(true)
    expect(scrollIntoView).toHaveBeenCalledWith({
      behavior: "smooth",
      block: "center"
    })
    expect(focus).toHaveBeenCalledWith({ preventScroll: true })
    expect(result.current.scrollToMessage("404")).toBe(false)
  })
})

function message(messageId: string): MessageView {
  return {
    message_id: messageId,
    author_id: "42",
    author: {
      id: "42",
      username: "myuu",
      global_name: "Myuu",
      display_name: "Myuu",
      avatar_hash: null,
      avatar_url: "/avatar.png",
      is_bot: false
    },
    message_reference_id: null,
    content: messageId,
    sticker_id: null,
    is_edited: false,
    revision_count: 1,
    is_deleted: false,
    deleted_by_id: null,
    deleted_by: null,
    created_at: "2023-11-14T22:13:20Z",
    updated_at: "2023-11-14T22:13:20Z"
  }
}
