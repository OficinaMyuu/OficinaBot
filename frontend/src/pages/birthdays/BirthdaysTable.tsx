import type { Birthday } from "@/types/birthday"
import type { UserSummary } from "@/types/user"

import { FiEdit2, FiTrash2 } from "react-icons/fi"
import { AppTooltip } from "@/components/ui/AppTooltip"
import { SortableHeader } from "@/components/ui/SortableHeader"
import { fallbackUser } from "@/stores/useUsersStore"
import { getDiscordDisplayName } from "@/utils/userUtils"
import { useTranslation } from "react-i18next"
import { useTableSort } from "@/utils/useTableSort"

import styles from "./BirthdaysTable.module.css"

type BirthdaysTableProps = {
  birthdays: Birthday[]
  usersById: Record<string, UserSummary>
  onEdit: (birthday: Birthday) => void
  onDelete: (birthday: Birthday) => void
}

export function BirthdaysTable({
  birthdays,
  usersById,
  onEdit,
  onDelete
}: BirthdaysTableProps) {
  const { t } = useTranslation()
  const { sorted, sortKey, sortDir, toggle } = useTableSort(birthdays)

  return (
    <table className={styles.table}>
      <thead>
        <tr>
          <SortableHeader
            label={t("birthdays.fields.name")}
            sortKey="name"
            activeSortKey={sortKey as string | null}
            sortDir={sortDir}
            onSort={(key) => toggle(key as keyof Birthday)}
          />
          <SortableHeader
            label={t("birthdays.fields.userId")}
            sortKey="user_id"
            activeSortKey={sortKey as string | null}
            sortDir={sortDir}
            onSort={(key) => toggle(key as keyof Birthday)}
          />
          <SortableHeader
            label={t("birthdays.fields.birthday")}
            sortKey="birthday"
            activeSortKey={sortKey as string | null}
            sortDir={sortDir}
            onSort={(key) => toggle(key as keyof Birthday)}
          />
          <SortableHeader
            label={t("birthdays.fields.zoneHours")}
            sortKey="zone_hours"
            activeSortKey={sortKey as string | null}
            sortDir={sortDir}
            onSort={(key) => toggle(key as keyof Birthday)}
          />
          <th aria-label={t("common.actions")} />
        </tr>
      </thead>
      <tbody>
        {sorted.map((birthday) => {
          const user =
            usersById[birthday.user_id] ?? fallbackUser(birthday.user_id)

          return (
            <tr key={birthday.user_id}>
              <td>
                <AppTooltip label={getDiscordDisplayName(user)}>
                  <span className={styles.user}>
                    <img src={user.avatar_url} alt="" draggable={false} />
                    <strong>{birthday.name}</strong>
                  </span>
                </AppTooltip>
              </td>
              <td className={styles.monospace}>{birthday.user_id}</td>
              <td>{formatBirthday(birthday.birthday)}</td>
              <td>{formatZone(birthday.zone_hours)}</td>
              <td>
                <div className={styles.rowActions}>
                  <AppTooltip label={t("birthdays.actions.edit")}>
                    <button
                      type="button"
                      aria-label={t("birthdays.actions.edit")}
                      onClick={() => onEdit(birthday)}
                    >
                      <FiEdit2 aria-hidden="true" />
                    </button>
                  </AppTooltip>
                  <AppTooltip label={t("birthdays.actions.delete")}>
                    <button
                      type="button"
                      aria-label={t("birthdays.actions.delete")}
                      onClick={() => onDelete(birthday)}
                    >
                      <FiTrash2 aria-hidden="true" />
                    </button>
                  </AppTooltip>
                </div>
              </td>
            </tr>
          )
        })}
      </tbody>
    </table>
  )
}

function formatBirthday(value: string): string {
  const [year, month, day] = value.split("-")
  return `${day}/${month}/${year}`
}

function formatZone(value: number): string {
  return value >= 0 ? `UTC+${value}` : `UTC${value}`
}
