import type { RefObject } from "react"
import type { ActionCost } from "@/types/actionCost"
import type { UserSummary } from "@/types/user"

import { ActionCostUpdatedBy } from "./ActionCostUpdatedBy"
import { SortableHeader } from "@/components/ui/SortableHeader"
import { CostCell } from "./CostCell"
import { useTranslation } from "react-i18next"
import { fallbackUser } from "@/stores/useUsersStore"
import { useTableSort } from "@/utils/useTableSort"

import styles from "./ActionCostsTable.module.css"

type ActionCostsTableProps = {
  items: ActionCost[]
  usersById: Record<string, UserSummary>
  editingItemType: ActionCost["item_type"] | null
  draftPrice: string
  inputRef: RefObject<HTMLInputElement | null>
  isSaving: boolean
  onBeginEditing: (item: ActionCost) => void
  onDraftChange: (value: string) => void
  onSave: (item: ActionCost) => void
  onCancelEditing: () => void
}

export function ActionCostsTable({
  items,
  usersById,
  editingItemType,
  draftPrice,
  inputRef,
  isSaving,
  onBeginEditing,
  onDraftChange,
  onSave,
  onCancelEditing
}: ActionCostsTableProps) {
  const { t } = useTranslation()
  const { sorted, sortKey, sortDir, toggle } = useTableSort(items)

  return (
    <table className={styles.table}>
      <thead>
        <tr>
          <SortableHeader
            label={t("economy.actionCosts.fields.action")}
            sortKey="item_type"
            activeSortKey={sortKey as string | null}
            sortDir={sortDir}
            onSort={(key) => toggle(key as keyof ActionCost)}
          />
          <th>{t("economy.actionCosts.fields.command")}</th>
          <SortableHeader
            label={t("economy.actionCosts.fields.cost")}
            sortKey="price"
            activeSortKey={sortKey as string | null}
            sortDir={sortDir}
            onSort={(key) => toggle(key as keyof ActionCost)}
          />
          <SortableHeader
            label={t("economy.actionCosts.fields.updatedAt")}
            sortKey="updated_at"
            activeSortKey={sortKey as string | null}
            sortDir={sortDir}
            onSort={(key) => toggle(key as keyof ActionCost)}
          />
        </tr>
      </thead>
      <tbody>
        {sorted.map((item) => {
          const itemKey = `economy.actionCosts.items.${item.item_type}`
          const updatedBy = item.updated_by
            ? (usersById[item.updated_by] ?? fallbackUser(item.updated_by))
            : null
          const isEditing = item.item_type === editingItemType

          return (
            <tr key={item.item_type}>
              <td>
                <strong>{t(`${itemKey}.title`)}</strong>
              </td>
              <td className={styles.command}>{t(`${itemKey}.command`)}</td>
              <td>
                <CostCell
                  item={item}
                  isEditing={isEditing}
                  isSaving={isEditing && isSaving}
                  draftPrice={draftPrice}
                  inputRef={inputRef}
                  onBeginEditing={() => onBeginEditing(item)}
                  onDraftChange={onDraftChange}
                  onSave={() => onSave(item)}
                  onCancel={onCancelEditing}
                />
              </td>
              <td>
                <ActionCostUpdatedBy
                  updatedAt={item.updated_at}
                  updatedBy={updatedBy}
                />
              </td>
            </tr>
          )
        })}
      </tbody>
    </table>
  )
}
