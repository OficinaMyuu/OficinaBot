import { useInfiniteQuery } from "@tanstack/react-query"
import { useDeferredValue, useEffect, useMemo, useState } from "react"
import { useTranslation } from "react-i18next"
import {
  FiChevronDown,
  FiChevronRight,
  FiRefreshCw,
  FiSearch
} from "react-icons/fi"
import { DashboardLayout } from "@/components/layout/DashboardLayout"
import { MessageRenderer } from "@/components/messages/MessageRenderer"
import { Button } from "@/components/ui/Button"
import { ticketService } from "@/services/ticketService"
import { useTicketsStore } from "@/stores/useTicketsStore"
import { fallbackUser, useUsersStore } from "@/stores/useUsersStore"
import type { ReactNode } from "react"
import type {
  Ticket,
  TicketListQuery,
  TicketMessage,
  TicketMessageView,
  TicketStatus
} from "@/types/ticket"
import type { UserSummary } from "@/types/user"
import { epochToDate } from "@/utils/time"
import styles from "./TicketsPage.module.css"

const ticketLimit = 25
const messageLimit = 50
const timeFormatter = new Intl.DateTimeFormat(undefined, {
  day: "2-digit",
  hour: "2-digit",
  minute: "2-digit",
  month: "2-digit",
  year: "numeric"
})

export function TicketsPage() {
  const { t } = useTranslation()
  const [search, setSearch] = useState("")
  const deferredSearch = useDeferredValue(search)
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
    () => ({ search: deferredSearch, status, limit: ticketLimit }),
    [deferredSearch, status]
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
          <label className={styles.search}>
            <FiSearch aria-hidden="true" />
            <input
              value={search}
              placeholder={t("tickets.searchPlaceholder")}
              onChange={(event) => setSearch(event.target.value)}
            />
          </label>

          <select
            value={status}
            onChange={(event) => setStatus(event.target.value as TicketStatus)}
            aria-label={t("tickets.filters.status")}
          >
            <option value="all">{t("tickets.filters.all")}</option>
            <option value="open">{t("tickets.filters.open")}</option>
            <option value="closed">{t("tickets.filters.closed")}</option>
          </select>

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
          <div className={styles.state}>{t("tickets.loading")}</div>
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
                    <span className={styles.user}>
                      {initiator.display_name}
                    </span>
                    <span
                      className={[styles.status, styles[ticket.status]].join(
                        " "
                      )}
                    >
                      {t(`tickets.status.${ticket.status}`)}
                    </span>
                    <time
                      dateTime={epochToDate(ticket.updated_at).toISOString()}
                    >
                      {formatTime(ticket.updated_at)}
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
                            value={<User user={initiator} />}
                          />
                          <Meta
                            label={t("tickets.fields.channel")}
                            value={ticket.channel_id}
                            mono
                          />
                          <Meta
                            label={t("tickets.fields.createdAt")}
                            value={formatTime(ticket.created_at)}
                          />
                          <Meta
                            label={t("tickets.fields.updatedAt")}
                            value={formatTime(ticket.updated_at)}
                          />
                          {closedBy ? (
                            <Meta
                              label={t("tickets.fields.closedBy")}
                              value={<User user={closedBy} />}
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

type MetaProps = {
  label: string
  value: ReactNode
  mono?: boolean
}

function Meta({ label, value, mono = false }: MetaProps) {
  return (
    <div>
      <dt>{label}</dt>
      <dd className={mono ? styles.mono : undefined}>{value}</dd>
    </div>
  )
}

function User({ user }: { user: UserSummary }) {
  return (
    <span className={styles.userValue}>
      <img src={user.avatar_url} alt="" />
      {user.display_name}
    </span>
  )
}

type TicketMessagesProps = {
  expanded: boolean
  loading: boolean
  error: string | null
  hasMore: boolean
  loadingMore: boolean
  messages: TicketMessageView[]
  onLoad: () => void
  onLoadMore: () => void
  onRetry: () => void
}

function TicketMessages({
  expanded,
  loading,
  error,
  hasMore,
  loadingMore,
  messages,
  onLoad,
  onLoadMore,
  onRetry
}: TicketMessagesProps) {
  const { t } = useTranslation()

  if (!expanded) {
    return (
      <div className={styles.messageGate}>
        <div className={styles.gatePreview} aria-hidden="true">
          <span />
          <span />
          <span />
        </div>
        <Button type="button" onClick={onLoad}>
          {t("tickets.actions.readMessages")}
        </Button>
      </div>
    )
  }

  if (loading) {
    return <MessageSkeleton label={t("tickets.loadingMessages")} />
  }

  if (error) {
    return (
      <div className={styles.messageState}>
        <p>{error}</p>
        <Button type="button" variant="secondary" onClick={onRetry}>
          <FiRefreshCw aria-hidden="true" />
          {t("common.refresh")}
        </Button>
      </div>
    )
  }

  return (
    <>
      <MessageRenderer messages={messages} />
      {hasMore ? (
        <Button
          className={styles.loadMoreMessages}
          type="button"
          variant="secondary"
          disabled={loadingMore}
          onClick={onLoadMore}
        >
          <FiChevronDown aria-hidden="true" />
          {t("tickets.actions.loadMoreMessages")}
        </Button>
      ) : null}
    </>
  )
}

function MessageSkeleton({ label }: { label: string }) {
  return (
    <div className={styles.skeleton} role="status" aria-label={label}>
      {[0, 1, 2].map((item) => (
        <div className={styles.skeletonRow} key={item}>
          <span />
          <div>
            <strong />
            <p />
          </div>
        </div>
      ))}
    </div>
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

function formatTime(value: number): string {
  return timeFormatter.format(epochToDate(value))
}

function toMessage(error: unknown): string {
  if (typeof error === "object" && error !== null && "message" in error) {
    return String(error.message)
  }
  if (error instanceof Error) {
    return error.message
  }
  return "Unexpected error"
}
