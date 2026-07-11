import { useEffect, useMemo, useState } from "react"
import type {
  Ticket,
  TicketListQuery,
  TicketMessage,
  TicketMessageView,
  TicketStatus
} from "@/types/ticket"
import type { UserSummary } from "@/types/user"
import { useTranslation } from "react-i18next"
import { FiChevronDown, FiChevronRight, FiRefreshCw } from "react-icons/fi"
import { useInfiniteQuery } from "@tanstack/react-query"
import { DashboardLayout } from "@/components/layout/DashboardLayout"
import { Button } from "@/components/ui/Button"
import { CustomSelect } from "@/components/ui/CustomSelect"
import { SearchInput } from "@/components/ui/SearchInput"
import { useDebouncedValue } from "@/hooks/useDebouncedValue"
import { ticketService } from "@/services/ticketService"
import { useTicketsStore } from "@/stores/useTicketsStore"
import { fallbackUser, useUsersStore } from "@/stores/useUsersStore"
import { formatLocalTimestamp } from "@/utils/timeUtils"
import { toMessage } from "@/utils/errorUtils"
import { Meta } from "./Meta"
import { TicketUser } from "./TicketUser"
import { TicketMessages } from "./TicketMessages"
import { TicketListSkeleton } from "./TicketListSkeleton"

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
    () => rawMessages.map((message) => toMessageView(message, usersById)),
    [rawMessages, usersById]
  )

  useEffect(() => {
    void fetchUsers(ticketUserIds(tickets))
  }, [fetchUsers, tickets])

  useEffect(() => {
    void fetchUsers(messageUserIds(rawMessages))
  }, [fetchUsers, rawMessages])

  const toggleTicket = (ticketId: number) => {
    setExpandedTicketId((current) => (current === ticketId ? null : ticketId))
    setMessagesTicketId(null)
  }

  return (
    <DashboardLayout title={t("tickets.title")}>
      <section className={styles.page}>
        <div className={styles.toolbar}>
          <SearchInput
            value={search}
            clearLabel={t("common.clearSearch")}
            aria-label={t("tickets.searchPlaceholder")}
            placeholder={t("tickets.searchPlaceholder")}
            onChange={(event) => setSearch(event.target.value)}
            onClear={() => setSearch("")}
          />

          <CustomSelect
            value={status}
            className={styles.filter}
            ariaLabel={t("tickets.filters.status")}
            options={[
              { value: "all", label: t("tickets.filters.all") },
              { value: "open", label: t("tickets.filters.open") },
              { value: "closed", label: t("tickets.filters.closed") }
            ]}
            onValueChange={setStatus}
          />

          <Button
            type="button"
            variant="secondary"
            onClick={() => void refreshTickets()}
          >
            <FiRefreshCw aria-hidden="true" />
            {t("common.refresh")}
          </Button>
        </div>

        {ticketsLoading ? (
          <TicketListSkeleton label={t("tickets.loading")} />
        ) : ticketError && tickets.length === 0 ? (
          <div className={styles.state}>{ticketError}</div>
        ) : tickets.length === 0 ? (
          <div className={styles.state}>{t("tickets.empty")}</div>
        ) : (
          <ol className={styles.entries} aria-label={t("tickets.listLabel")}>
            {tickets.map((ticket) => {
              const expanded = expandedTicketId === ticket.id
              const messagesRequested = messagesTicketId === ticket.id
              const initiator =
                usersById[ticket.initiator_id] ??
                fallbackUser(ticket.initiator_id)
              const closedBy = ticket.closed_by_id
                ? (usersById[ticket.closed_by_id] ??
                  fallbackUser(ticket.closed_by_id))
                : null

              return (
                <li className={styles.entry} key={ticket.id}>
                  <button
                    className={styles.summary}
                    type="button"
                    aria-expanded={expanded}
                    onClick={() => toggleTicket(ticket.id)}
                  >
                    <FiChevronRight
                      className={expanded ? styles.expandedIcon : undefined}
                      aria-hidden="true"
                    />
                    <img
                      className={styles.avatar}
                      src={initiator.avatar_url}
                      alt=""
                    />
                    <span className={styles.ticketTitle}>
                      <strong>{formatTicketNumber(ticket.id)}</strong>
                      <span>{ticket.title}</span>
                    </span>
                    <span className={styles.user}>{initiator.username}</span>
                    <span
                      className={[styles.status, styles[ticket.status]].join(
                        " "
                      )}
                    >
                      {t(`tickets.status.${ticket.status}`)}
                    </span>
                    <time dateTime={ticket.updated_at}>
                      {formatLocalTimestamp(ticket.updated_at)}
                    </time>
                  </button>

                  {expanded ? (
                    <div className={styles.expanded}>
                      <div className={styles.details}>
                        <p className={styles.description}>
                          {ticket.description}
                        </p>
                        {ticket.status === "open" ? (
                          <p className={styles.warning}>
                            {t("tickets.openWarning")}
                          </p>
                        ) : null}
                        <dl className={styles.metaGrid}>
                          <Meta
                            label={t("tickets.fields.initiator")}
                            value={<TicketUser user={initiator} />}
                          />
                          <Meta
                            label={t("tickets.fields.channel")}
                            value={ticket.channel_id}
                            mono
                          />
                          <Meta
                            label={t("tickets.fields.createdAt")}
                            value={formatLocalTimestamp(ticket.created_at)}
                          />
                          <Meta
                            label={t("tickets.fields.updatedAt")}
                            value={formatLocalTimestamp(ticket.updated_at)}
                          />
                          {closedBy ? (
                            <Meta
                              label={t("tickets.fields.closedBy")}
                              value={<TicketUser user={closedBy} />}
                            />
                          ) : null}
                          {ticket.close_reason ? (
                            <Meta
                              label={t("tickets.fields.closeReason")}
                              value={ticket.close_reason}
                            />
                          ) : null}
                          {ticket.merged_into ? (
                            <Meta
                              label={t("tickets.fields.mergedInto")}
                              value={formatTicketNumber(ticket.merged_into)}
                            />
                          ) : null}
                        </dl>
                      </div>

                      <div className={styles.transcript}>
                        <TicketMessages
                          expanded={messagesRequested}
                          loading={messagesQuery.isLoading}
                          error={
                            messagesQuery.isError
                              ? toMessage(messagesQuery.error)
                              : null
                          }
                          hasMore={Boolean(messagesQuery.hasNextPage)}
                          loadingMore={messagesQuery.isFetchingNextPage}
                          messages={messagesRequested ? messages : []}
                          onLoad={() => setMessagesTicketId(ticket.id)}
                          onLoadMore={() => void messagesQuery.fetchNextPage()}
                          onRetry={() => void messagesQuery.refetch()}
                        />
                      </div>
                    </div>
                  ) : null}
                </li>
              )
            })}
          </ol>
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

function toMessageView(
  message: TicketMessage,
  usersById: Record<string, UserSummary>
): TicketMessageView {
  return {
    ...message,
    author: usersById[message.author_id] ?? fallbackUser(message.author_id),
    deleted_by: message.deleted_by_id
      ? (usersById[message.deleted_by_id] ??
        fallbackUser(message.deleted_by_id))
      : null
  }
}

function ticketUserIds(tickets: Ticket[]): string[] {
  return tickets.flatMap(
    (ticket) =>
      [ticket.initiator_id, ticket.closed_by_id].filter(Boolean) as string[]
  )
}

function messageUserIds(messages: TicketMessage[]): string[] {
  return messages.flatMap(
    (message) =>
      [message.author_id, message.deleted_by_id].filter(Boolean) as string[]
  )
}

function formatTicketNumber(id: number): string {
  return `#${String(id).padStart(2, "0")}`
}
