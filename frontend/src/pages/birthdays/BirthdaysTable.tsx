import { useTranslation } from "react-i18next"
import { FiEdit2, FiTrash2 } from "react-icons/fi"
import { SortableHeader } from "@/components/ui/SortableHeader"
import type { Birthday } from "@/types/birthday"
import { useTableSort } from "@/utils/useTableSort"
import styles from "./BirthdaysTable.module.css"

type BirthdaysTableProps = {
  birthdays: Birthday[]
  onEdit: (birthday: Birthday) => void
  onDelete: (birthday: Birthday) => void
}

export function BirthdaysTable({ birthdays, onEdit, onDelete }: BirthdaysTableProps) {
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
        {sorted.map((birthday) => (
          <tr key={birthday.user_id}>
            <td>
              <strong>{birthday.name}</strong>
            </td>
            <td className={styles.monospace}>{birthday.user_id}</td>
            <td>{formatBirthday(birthday.birthday)}</td>
            <td>{formatZone(birthday.zone_hours)}</td>
            <td>
              <div className={styles.rowActions}>
                <button
                  type="button"
                  onClick={() => onEdit(birthday)}
                  title={t("birthdays.actions.edit")}
                >
                  <FiEdit2 aria-hidden="true" />
                </button>
                <button
                  type="button"
                  onClick={() => onDelete(birthday)}
                  title={t("birthdays.actions.delete")}
                >
                  <FiTrash2 aria-hidden="true" />
                </button>
              </div>
            </td>
          </tr>
        ))}
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
