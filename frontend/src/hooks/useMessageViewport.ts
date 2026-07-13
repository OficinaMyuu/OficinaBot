import { useCallback, useLayoutEffect, useRef } from "react"
import type { UIEventHandler } from "react"
import type { MessageView } from "@/types/message"

type MessageViewportOptions = {
  channelId: string
  messages: MessageView[]
  hasMoreBefore: boolean
  loadingMore: boolean
  onLoadOlder: () => void
}

export function useMessageViewport({
  channelId,
  messages,
  hasMoreBefore,
  loadingMore,
  onLoadOlder
}: MessageViewportOptions) {
  const viewportRef = useRef<HTMLDivElement>(null)
  const snapshotRef = useRef({
    channelId: "",
    firstMessageId: "",
    lastMessageId: "",
    scrollHeight: 0
  })

  useLayoutEffect(() => {
    const viewport = viewportRef.current
    if (!viewport || messages.length === 0) return
    const firstMessageId = messages[0].message_id
    const lastMessageId = messages[messages.length - 1].message_id
    const previous = snapshotRef.current
    const isInitialPage =
      previous.channelId !== channelId || previous.lastMessageId === ""
    const prependedOlderMessages =
      previous.channelId === channelId &&
      previous.lastMessageId === lastMessageId &&
      previous.firstMessageId !== firstMessageId

    if (isInitialPage) {
      viewport.scrollTop = viewport.scrollHeight
    } else if (prependedOlderMessages) {
      viewport.scrollTop += viewport.scrollHeight - previous.scrollHeight
    }

    snapshotRef.current = {
      channelId,
      firstMessageId,
      lastMessageId,
      scrollHeight: viewport.scrollHeight
    }
  }, [channelId, messages])

  const onScroll = useCallback<UIEventHandler<HTMLDivElement>>(
    (event) => {
      const viewport = event.currentTarget
      if (
        viewport.scrollTop <= 64 &&
        viewport.scrollHeight > viewport.clientHeight &&
        hasMoreBefore &&
        !loadingMore
      ) {
        onLoadOlder()
      }
    },
    [hasMoreBefore, loadingMore, onLoadOlder]
  )

  return { viewportRef, onScroll }
}
