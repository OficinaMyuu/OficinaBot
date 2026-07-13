import { useEffect, useMemo, useState } from "react"
import type { TicketListQuery, TicketStatus } from "@/types/ticket"

import { TicketList } from "./TicketList"
import { TicketListSkeleton } from "./TicketListSkeleton"
import { TicketsToolbar } from "./TicketsToolbar"
import { FiChevronDown } from "react-icons/fi"
import { DashboardLayout } from "@/components/layout/DashboardLayout"
import { Button } from "@/components/ui/Button"
import { useDebouncedValue } from "@/hooks/useDebouncedValue"
import { useTicketsStore } from "@/stores/useTicketsStore"
import { useUsersStore } from "@/stores/useUsersStore"
import { useTranslation } from "react-i18next"
import { ticketUserIds } from "@/utils/ticketUtils"

import styles from "./TicketsPage.module.css"

const ticketLimit = 25

export function TicketsPage() {
  const { t } = useTranslation()
  const [search, setSearch] = useState("")
  const debouncedSearch = useDebouncedValue(search)
  const [status, setStatus] = useState<TicketStatus>("all")
  const [expandedTicketId, setExpandedTicketId] = useState<number | null>(null)
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

  useEffect(() => {
    void fetchUsers(ticketUserIds(tickets))
  }, [fetchUsers, tickets])

  const toggleTicket = (ticketId: number) => {
    setExpandedTicketId((current) => (current === ticketId ? null : ticketId))
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
            listLabel={t("tickets.listLabel")}
            onToggleTicket={toggleTicket}
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
