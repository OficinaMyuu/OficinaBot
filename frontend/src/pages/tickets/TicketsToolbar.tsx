import { useTranslation } from "react-i18next"
import { FiRefreshCw } from "react-icons/fi"
import { Button } from "@/components/ui/Button"
import { CustomSelect } from "@/components/ui/CustomSelect"
import { SearchInput } from "@/components/ui/SearchInput"
import type { TicketStatus } from "@/types/ticket"
import styles from "./TicketsToolbar.module.css"

type TicketsToolbarProps = {
  search: string
  status: TicketStatus
  onSearchChange: (value: string) => void
  onStatusChange: (status: TicketStatus) => void
  onRefresh: () => void
}

export function TicketsToolbar({
  search,
  status,
  onSearchChange,
  onStatusChange,
  onRefresh
}: TicketsToolbarProps) {
  const { t } = useTranslation()

  return (
    <div className={styles.toolbar}>
      <SearchInput
        value={search}
        clearLabel={t("common.clearSearch")}
        aria-label={t("tickets.searchPlaceholder")}
        placeholder={t("tickets.searchPlaceholder")}
        onChange={(event) => onSearchChange(event.target.value)}
        onClear={() => onSearchChange("")}
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
        onValueChange={onStatusChange}
      />

      <Button type="button" variant="secondary" onClick={onRefresh}>
        <FiRefreshCw aria-hidden="true" />
        {t("common.refresh")}
      </Button>
    </div>
  )
}
