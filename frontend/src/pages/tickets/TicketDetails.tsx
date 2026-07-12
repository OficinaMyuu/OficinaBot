import { useTranslation } from "react-i18next"
import type { Ticket } from "@/types/ticket"
import type { UserSummary } from "@/types/user"
import { formatLocalTimestamp } from "@/utils/timeUtils"
import { formatTicketNumber } from "@/utils/ticketUtils"
import { Meta } from "./Meta"
import { TicketUser } from "./TicketUser"
import styles from "./TicketDetails.module.css"

type TicketDetailsProps = {
  ticket: Ticket
  initiator: UserSummary
  closedBy: UserSummary | null
}

export function TicketDetails({
  ticket,
  initiator,
  closedBy
}: TicketDetailsProps) {
  const { t } = useTranslation()

  return (
    <div className={styles.details}>
      <p className={styles.description}>{ticket.description}</p>
      {ticket.status === "open" ? (
        <p className={styles.warning}>{t("tickets.openWarning")}</p>
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
  )
}
