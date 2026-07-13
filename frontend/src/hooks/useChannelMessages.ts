import { useEffect, useMemo } from "react"
import { useInfiniteQuery } from "@tanstack/react-query"
import { messageService } from "@/services/messageService"
import { useGuildDirectoryStore } from "@/stores/useGuildDirectoryStore"
import { fallbackUser, useUsersStore } from "@/stores/useUsersStore"
import { messageUserIds, toMessageViews } from "@/utils/messageUtils"

const messageLimit = 50

export function useChannelMessages(channelId: string, enabled: boolean) {
  const usersById = useUsersStore((state) => state.usersById)
  const fetchUsers = useUsersStore((state) => state.fetchUsers)
  const loadGuildDirectory = useGuildDirectoryStore((state) => state.load)
  const query = useInfiniteQuery({
    queryKey: ["channel-messages", channelId],
    queryFn: ({ pageParam }) =>
      messageService.list(
        channelId,
        pageParam
          ? { limit: messageLimit, before: pageParam }
          : { limit: messageLimit }
      ),
    initialPageParam: undefined as string | undefined,
    getNextPageParam: (lastPage) =>
      lastPage.has_more_before ? lastPage.messages[0]?.message_id : undefined,
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

  return { ...query, messages, usersById }
}
