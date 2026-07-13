import { useTranslation } from "react-i18next"
import { FiChevronDown, FiRefreshCw } from "react-icons/fi"
import { MessageRenderer } from "@/components/messages/MessageRenderer"
import { Button } from "@/components/ui/Button"
import type { TicketMessageView } from "@/types/ticket"
import { MessageSkeleton } from "./MessageSkeleton"
import styles from "./TicketMessages.module.css"

type TicketMessagesProps = {
  ticketId: number
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

export function TicketMessages({
  ticketId,
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
    <div className={styles.panel}>
      <div className={styles.viewport}>
        <MessageRenderer ticketId={ticketId} messages={messages} />
      </div>
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
    </div>
  )
}
