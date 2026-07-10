import type { RefObject } from "react"
import { Spinner } from "@/components/ui/loaders"
import type { ActionCost } from "@/types/actionCost"
import { formatIntegerInput, formatNumber } from "@/utils/numberUtils"
import styles from "./CostCell.module.css"

type CostCellProps = {
  item: ActionCost
  isEditing: boolean
  isSaving: boolean
  draftPrice: string
  inputRef: RefObject<HTMLInputElement | null>

  onBeginEditing: () => void
  onDraftChange: (value: string) => void
  onSave: () => void
  onCancel: () => void
}

export function CostCell({
  item,
  isEditing,
  isSaving,
  draftPrice,
  inputRef,
  onBeginEditing,
  onDraftChange,
  onSave,
  onCancel
}: CostCellProps) {
  if (isSaving) {
    return (
      <div className={styles.cellSpinner}>
        <Spinner size={18} />
      </div>
    )
  }

  if (isEditing) {
    return (
      <label className={styles.priceInput}>
        <span className={styles.srOnly}>{item.item_type}</span>
        <input
          ref={inputRef}
          aria-label={item.item_type}
          inputMode="numeric"
          type="text"
          value={draftPrice}
          onChange={(event) =>
            onDraftChange(formatIntegerInput(event.target.value))
          }
          onBlur={onSave}
          onKeyDown={(event) => {
            if (event.key === "Enter") {
              event.preventDefault()
              onSave()
            } else if (event.key === "Escape") {
              event.preventDefault()
              onCancel()
            }
          }}
        />
      </label>
    )
  }

  return (
    <span
      className={styles.editableCell}
      role="button"
      tabIndex={0}
      onClick={onBeginEditing}
      onKeyDown={(event) => {
        if (event.key === "Enter" || event.key === " ") {
          event.preventDefault()
          onBeginEditing()
        }
      }}
    >
      {formatNumber(item.price)}
    </span>
  )
}
