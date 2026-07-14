import { useCallback, useEffect, useMemo, useState } from "react"
import { useInfiniteQuery } from "@tanstack/react-query"
import { messageService } from "@/services/messageService"
import { useGuildDirectoryStore } from "@/stores/useGuildDirectoryStore"
import { fallbackUser, useUsersStore } from "@/stores/useUsersStore"
import { messageUserIds, toMessageViews } from "@/utils/messageUtils"
import type { ChannelMessagesQuery } from "@/types/message"

const messageLimit = 50
type MessageAnchor = ChannelMessagesQuery

type AroundTarget = {
  channelId: string
  messageId: string
}

export function useChannelMessages(channelId: string, enabled: boolean) {
  const [aroundTarget, setAroundTarget] = useState<AroundTarget | null>(null)
  const usersById = useUsersStore((state) => state.usersById)
  const fetchUsers = useUsersStore((state) => state.fetchUsers)
  const loadGuildDirectory = useGuildDirectoryStore((state) => state.load)
  const aroundMessageId =
    aroundTarget?.channelId === channelId ? aroundTarget.messageId : null
  const query = useInfiniteQuery({
    queryKey: ["channel-messages", channelId, aroundMessageId],
    queryFn: ({ pageParam }) =>
      messageService.list(channelId, { ...pageParam, limit: messageLimit }),
    initialPageParam: aroundMessageId
      ? ({ around: aroundMessageId } as MessageAnchor)
      : ({} as MessageAnchor),
    getNextPageParam: (lastPage) =>
      lastPage.has_more_before && lastPage.messages[0]
        ? ({ before: lastPage.messages[0].message_id } as MessageAnchor)
        : undefined,
    getPreviousPageParam: (firstPage) => {
      const lastMessage = firstPage.messages[firstPage.messages.length - 1]
      return firstPage.has_more_after && lastMessage
        ? ({ after: lastMessage.message_id } as MessageAnchor)
        : undefined
    },
    enabled,
    gcTime: 0
  })
  const rawMessages = useMemo(
    () =>
      [...(query.data?.pages ?? [])].reverse().flatMap((page) => page.messages),
    [query.data]
  )
  const messages = useMemo(
    () => toMessageViews(rawMessages, usersById, fallbackUser),
    [rawMessages, usersById]
  )

  useEffect(() => {
    if (enabled) void fetchUsers(messageUserIds(rawMessages))
  }, [enabled, fetchUsers, rawMessages])

  useEffect(() => {
    if (enabled) void loadGuildDirectory()
  }, [enabled, loadGuildDirectory])

  const jumpToMessage = useCallback(
    (messageId: string) => setAroundTarget({ channelId, messageId }),
    [channelId]
  )

  return { ...query, jumpToMessage, messages, usersById }
}
