import { useEffect, useMemo, useState } from "react"
import type { TicketListQuery, TicketStatus } from "@/types/ticket"

import { TicketList } from "./TicketList"
import { TicketListSkeleton } from "./TicketListSkeleton"
import { TicketsToolbar } from "./TicketsToolbar"
import { FiChevronDown } from "react-icons/fi"
import { DashboardLayout } from "@/components/layout/DashboardLayout"
import { Button } from "@/components/ui/Button"
import { useDebouncedValue } from "@/hooks/useDebouncedValue"
import { useInfiniteQuery } from "@tanstack/react-query"
import { ticketService } from "@/services/ticketService"
import { useTicketsStore } from "@/stores/useTicketsStore"
import { fallbackUser, useUsersStore } from "@/stores/useUsersStore"
import { useGuildDirectoryStore } from "@/stores/useGuildDirectoryStore"
import { toMessage } from "@/utils/errorUtils"
import { useTranslation } from "react-i18next"
import {
  messageUserIds,
  ticketUserIds,
  toTicketMessageViews
} from "@/utils/ticketUtils"

import styles from "./TicketsPage.module.css"

const ticketLimit = 25
const messageLimit = 50

export function TicketsPage() {
  const { t } = useTranslation()
  const [search, setSearch] = useState("")
  const debouncedSearch = useDebouncedValue(search)
  const [status, setStatus] = useState<TicketStatus>("all")
  const [expandedTicketId, setExpandedTicketId] = useState<number | null>(null)
  const [messagesTicketId, setMessagesTicketId] = useState<number | null>(null)
  const tickets = useTicketsStore((state) => state.tickets)
  const nextTicketCursor = useTicketsStore((state) => state.nextCursor)
  const ticketsLoading = useTicketsStore((state) => state.isLoading)
  const ticketsLoadingMore = useTicketsStore((state) => state.isLoadingMore)
  const ticketError = useTicketsStore((state) => state.error)
  const loadTickets = useTicketsStore((state) => state.load)
  const loadMoreTickets = useTicketsStore((state) => state.loadMore)
  const refreshTickets = useTicketsStore((state) => state.refresh)
  const usersById = useUsersStore((state) => state.usersById)
  const fetchUsers = useUsersStore((state) => state.fetchUsers)
  const loadGuildDirectory = useGuildDirectoryStore((state) => state.load)

  const ticketQuery = useMemo<TicketListQuery>(
    () => ({ search: debouncedSearch, status, limit: ticketLimit }),
    [debouncedSearch, status]
  )

  useEffect(() => {
    void loadTickets(ticketQuery)
  }, [loadTickets, ticketQuery])

  const messagesQuery = useInfiniteQuery({
    queryKey: ["ticket-messages", messagesTicketId],
    queryFn: ({ pageParam }) => {
      if (messagesTicketId === null) {
        throw new Error("Ticket not selected")
      }
      return ticketService.messages(messagesTicketId, {
        limit: messageLimit,
        cursor: typeof pageParam === "string" ? pageParam : undefined
      })
    },
    enabled: messagesTicketId !== null,
    initialPageParam: null as string | null,
    getNextPageParam: (lastPage) => lastPage.next_cursor,
    gcTime: 0
  })

  const rawMessages = useMemo(
    () => messagesQuery.data?.pages.flatMap((page) => page.messages) ?? [],
    [messagesQuery.data]
  )
  const messages = useMemo(
    () => toTicketMessageViews(rawMessages, usersById, fallbackUser),
    [rawMessages, usersById]
  )

  useEffect(() => {
    void fetchUsers(ticketUserIds(tickets))
  }, [fetchUsers, tickets])

  useEffect(() => {
    void fetchUsers(messageUserIds(rawMessages))
  }, [fetchUsers, rawMessages])

  useEffect(() => {
    if (messagesTicketId !== null) void loadGuildDirectory()
  }, [loadGuildDirectory, messagesTicketId])

  const toggleTicket = (ticketId: number) => {
    setExpandedTicketId((current) => (current === ticketId ? null : ticketId))
    setMessagesTicketId(null)
  }

  return (
    <DashboardLayout title={t("tickets.title")}>
      <section className={styles.page}>
        <TicketsToolbar
          search={search}
          status={status}
          onSearchChange={setSearch}
          onStatusChange={setStatus}
          onRefresh={() => void refreshTickets()}
        />

        {ticketsLoading ? (
          <TicketListSkeleton label={t("tickets.loading")} />
        ) : ticketError && tickets.length === 0 ? (
          <div className={styles.state}>{ticketError}</div>
        ) : tickets.length === 0 ? (
          <div className={styles.state}>{t("tickets.empty")}</div>
        ) : (
          <TicketList
            tickets={tickets}
            usersById={usersById}
            expandedTicketId={expandedTicketId}
            messagesTicketId={messagesTicketId}
            messages={messages}
            messagesLoading={messagesQuery.isLoading}
            messagesError={
              messagesQuery.isError ? toMessage(messagesQuery.error) : null
            }
            hasMoreMessages={Boolean(messagesQuery.hasNextPage)}
            loadingMoreMessages={messagesQuery.isFetchingNextPage}
            listLabel={t("tickets.listLabel")}
            onToggleTicket={toggleTicket}
            onLoadMessages={setMessagesTicketId}
            onLoadMoreMessages={() => void messagesQuery.fetchNextPage()}
            onRetryMessages={() => void messagesQuery.refetch()}
          />
        )}

        {ticketError && tickets.length > 0 ? (
          <div className={styles.state}>{ticketError}</div>
        ) : null}

        {nextTicketCursor ? (
          <Button
            className={styles.loadMore}
            type="button"
            variant="secondary"
            disabled={ticketsLoadingMore}
            onClick={() => void loadMoreTickets()}
          >
            <FiChevronDown aria-hidden="true" />
            {t("tickets.actions.loadMore")}
          </Button>
        ) : null}
      </section>
    </DashboardLayout>
  )
}
