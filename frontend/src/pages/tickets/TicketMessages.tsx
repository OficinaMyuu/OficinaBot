import { useState } from "react"
import { useTranslation } from "react-i18next"
import { FiChevronUp, FiRefreshCw } from "react-icons/fi"
import { MessageRenderer } from "@/components/messages/MessageRenderer"
import { Button } from "@/components/ui/Button"
import { useChannelMessages } from "@/hooks/useChannelMessages"
import { useMessageViewport } from "@/hooks/useMessageViewport"
import { toMessage } from "@/utils/errorUtils"
import { MessageSkeleton } from "./MessageSkeleton"
import styles from "./TicketMessages.module.css"

type TicketMessagesProps = {
  channelId: string
}

export function TicketMessages({ channelId }: TicketMessagesProps) {
  const { t } = useTranslation()
  const [requested, setRequested] = useState(false)
  const history = useChannelMessages(channelId, requested)
  const loadOlder = () => void history.fetchNextPage()
  const { viewportRef, onScroll } = useMessageViewport({
    channelId,
    messages: history.messages,
    hasMoreBefore: Boolean(history.hasNextPage),
    loadingMore: history.isFetchingNextPage,
    onLoadOlder: loadOlder
  })

  if (!requested) {
    return (
      <div className={styles.messageGate}>
        <div className={styles.gatePreview} aria-hidden="true">
          <span />
          <span />
          <span />
        </div>
        <Button type="button" onClick={() => setRequested(true)}>
          {t("tickets.actions.readMessages")}
        </Button>
      </div>
    )
  }

  if (history.isLoading) {
    return <MessageSkeleton label={t("tickets.loadingMessages")} />
  }

  if (history.isError) {
    return (
      <div className={styles.messageState}>
        <p>{toMessage(history.error)}</p>
        <Button
          type="button"
          variant="secondary"
          onClick={() => void history.refetch()}
        >
          <FiRefreshCw aria-hidden="true" />
          {t("common.refresh")}
        </Button>
      </div>
    )
  }

  return (
    <div className={styles.panel}>
      <div className={styles.viewport} ref={viewportRef} onScroll={onScroll}>
        {history.hasNextPage ? (
          <Button
            className={styles.loadMoreMessages}
            type="button"
            variant="secondary"
            disabled={history.isFetchingNextPage}
            onClick={loadOlder}
          >
            <FiChevronUp aria-hidden="true" />
            {t("tickets.actions.loadMoreMessages")}
          </Button>
        ) : null}
        <MessageRenderer
          channelId={channelId}
          messages={history.messages}
          usersById={history.usersById}
        />
      </div>
    </div>
  )
}
