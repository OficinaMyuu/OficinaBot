import { useInfiniteQuery } from '@tanstack/react-query'
import { useDeferredValue, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { FiChevronDown, FiRefreshCw, FiSearch } from 'react-icons/fi'
import { MessageRenderer } from '@/components/messages/MessageRenderer'
import { DashboardLayout } from '@/components/layout/DashboardLayout'
import { Button } from '@/components/ui/Button'
import { ticketService } from '@/services/ticketService'
import type { Ticket, TicketListQuery, TicketStatus } from '@/types/ticket'
import { epochToDate } from '@/utils/time'
import styles from './TicketsPage.module.css'

const ticketLimit = 25
const messageLimit = 50
const timeFormatter = new Intl.DateTimeFormat(undefined, {
  day: '2-digit',
  hour: '2-digit',
  minute: '2-digit',
  month: '2-digit',
  year: 'numeric',
})

export function TicketsPage() {
  const { t } = useTranslation()
  const [search, setSearch] = useState('')
  const deferredSearch = useDeferredValue(search)
  const [status, setStatus] = useState<TicketStatus>('all')
  const [selectedTicket, setSelectedTicket] = useState<Ticket | null>(null)

  const ticketQuery = useMemo<TicketListQuery>(
    () => ({ search: deferredSearch, status, limit: ticketLimit }),
    [deferredSearch, status],
  )

  const ticketsQuery = useInfiniteQuery({
    queryKey: ['tickets', ticketQuery],
    queryFn: ({ pageParam }) =>
      ticketService.list({
        ...ticketQuery,
        cursor: typeof pageParam === 'string' ? pageParam : undefined,
      }),
    initialPageParam: null as string | null,
    getNextPageParam: (lastPage) => lastPage.next_cursor,
  })

  const selectedTicketId = selectedTicket?.id ?? null
  const messagesQuery = useInfiniteQuery({
    queryKey: ['ticket-messages', selectedTicketId],
    queryFn: ({ pageParam }) => {
      if (selectedTicketId === null) {
        throw new Error('Ticket not selected')
      }
      return ticketService.messages(selectedTicketId, {
        limit: messageLimit,
        cursor: typeof pageParam === 'string' ? pageParam : undefined,
      })
    },
    enabled: selectedTicketId !== null,
    initialPageParam: null as string | null,
    getNextPageParam: (lastPage) => lastPage.next_cursor,
  })

  const tickets = useMemo(() => ticketsQuery.data?.pages.flatMap((page) => page.tickets) ?? [], [ticketsQuery.data])
  const messages = useMemo(() => messagesQuery.data?.pages.flatMap((page) => page.messages) ?? [], [messagesQuery.data])
  const activeTicket = messagesQuery.data?.pages[0]?.ticket ?? selectedTicket

  return (
    <DashboardLayout title={t('tickets.title')}>
      <section className={styles.page}>
        <div className={styles.toolbar}>
          <label className={styles.search}>
            <FiSearch aria-hidden="true" />
            <input
              value={search}
              placeholder={t('tickets.searchPlaceholder')}
              onChange={(event) => setSearch(event.target.value)}
            />
          </label>

          <select value={status} onChange={(event) => setStatus(event.target.value as TicketStatus)} aria-label={t('tickets.filters.status')}>
            <option value="all">{t('tickets.filters.all')}</option>
            <option value="open">{t('tickets.filters.open')}</option>
            <option value="closed">{t('tickets.filters.closed')}</option>
          </select>

          <Button type="button" variant="secondary" onClick={() => void ticketsQuery.refetch()}>
            <FiRefreshCw aria-hidden="true" />
            {t('common.refresh')}
          </Button>
        </div>

        <div className={styles.workspace}>
          <section className={styles.ticketPanel} aria-label={t('tickets.listLabel')}>
            <div className={styles.tableShell}>
              {ticketsQuery.isLoading ? (
                <div className={styles.state}>{t('tickets.loading')}</div>
              ) : ticketsQuery.isError ? (
                <div className={styles.state}>{toMessage(ticketsQuery.error)}</div>
              ) : tickets.length === 0 ? (
                <div className={styles.state}>{t('tickets.empty')}</div>
              ) : (
                <table className={styles.table}>
                  <thead>
                    <tr>
                      <th>{t('tickets.fields.ticket')}</th>
                      <th>{t('tickets.fields.initiator')}</th>
                      <th>{t('tickets.fields.status')}</th>
                      <th>{t('tickets.fields.updatedAt')}</th>
                      <th aria-label={t('common.actions')} />
                    </tr>
                  </thead>
                  <tbody>
                    {tickets.map((ticket) => (
                      <tr className={selectedTicket?.id === ticket.id ? styles.selectedRow : undefined} key={ticket.id}>
                        <td>
                          <strong>{formatTicketNumber(ticket.id)}</strong>
                          <span>{ticket.title}</span>
                        </td>
                        <td>{ticket.initiator.display_name}</td>
                        <td>
                          <span className={[styles.status, styles[ticket.status]].join(' ')}>{t(`tickets.status.${ticket.status}`)}</span>
                        </td>
                        <td>{formatTime(ticket.updated_at)}</td>
                        <td>
                          <Button className={styles.compactButton} type="button" variant="secondary" onClick={() => setSelectedTicket(ticket)}>
                            {t('tickets.actions.readMessages')}
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>

            {ticketsQuery.hasNextPage ? (
              <Button
                className={styles.loadMore}
                type="button"
                variant="secondary"
                disabled={ticketsQuery.isFetchingNextPage}
                onClick={() => void ticketsQuery.fetchNextPage()}
              >
                <FiChevronDown aria-hidden="true" />
                {t('tickets.actions.loadMore')}
              </Button>
            ) : null}
          </section>

          <section className={styles.messagePanel} aria-label={t('tickets.messagesLabel')}>
            <div className={styles.messageHeader}>
              <div>
                <strong>{activeTicket ? `${formatTicketNumber(activeTicket.id)} ${activeTicket.title}` : t('tickets.noSelection')}</strong>
                {activeTicket ? <span>{t('tickets.channel', { id: activeTicket.channel_id })}</span> : null}
              </div>
              {activeTicket ? (
                <Button type="button" variant="secondary" onClick={() => void messagesQuery.refetch()}>
                  <FiRefreshCw aria-hidden="true" />
                  {t('common.refresh')}
                </Button>
              ) : null}
            </div>

            {activeTicket?.status === 'open' ? <p className={styles.warning}>{t('tickets.openWarning')}</p> : null}

            <div className={styles.messages}>
              {selectedTicketId === null ? (
                <div className={styles.state}>{t('tickets.noSelection')}</div>
              ) : messagesQuery.isLoading ? (
                <div className={styles.state}>{t('tickets.loadingMessages')}</div>
              ) : messagesQuery.isError ? (
                <div className={styles.state}>{toMessage(messagesQuery.error)}</div>
              ) : (
                <MessageRenderer messages={messages} />
              )}
            </div>

            {messagesQuery.hasNextPage ? (
              <Button
                className={styles.loadMore}
                type="button"
                variant="secondary"
                disabled={messagesQuery.isFetchingNextPage}
                onClick={() => void messagesQuery.fetchNextPage()}
              >
                <FiChevronDown aria-hidden="true" />
                {t('tickets.actions.loadMoreMessages')}
              </Button>
            ) : null}
          </section>
        </div>
      </section>
    </DashboardLayout>
  )
}

function formatTicketNumber(id: number): string {
  return `#${String(id).padStart(2, '0')}`
}

function formatTime(value: number): string {
  return timeFormatter.format(epochToDate(value))
}

function toMessage(error: unknown): string {
  if (typeof error === 'object' && error !== null && 'message' in error) {
    return String(error.message)
  }
  if (error instanceof Error) {
    return error.message
  }
  return 'Unexpected error'
}
