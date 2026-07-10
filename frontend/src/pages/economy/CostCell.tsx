import { useRef } from "react"
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
  const isCancelledRef = useRef(false)

  if (isSaving) {
    return (
      <div className={styles.cellSpinner}>
        <Spinner size={18} />
      </div>
    )
  }

  const displayValue = isEditing ? draftPrice : formatNumber(item.price)

  return (
    <label className={styles.cellWrapper}>
      <span className={styles.srOnly}>{item.item_type}</span>
      <input
        ref={isEditing ? inputRef : null}
        className={isEditing ? styles.inputActive : styles.inputResting}
        aria-label={item.item_type}
        inputMode="numeric"
        type="text"
        value={displayValue}
        onFocus={() => {
          if (!isEditing) {
            onBeginEditing()
          }
        }}
        onClick={() => {
          if (!isEditing) {
            onBeginEditing()
          }
        }}
        onChange={(event) => {
          if (!isEditing) {
            onBeginEditing()
          }
          onDraftChange(formatIntegerInput(event.target.value))
        }}
        onBlur={() => {
          if (isCancelledRef.current) {
            isCancelledRef.current = false
            return
          }
          onSave()
        }}
        onKeyDown={(event) => {
          if (event.key === "Enter") {
            event.preventDefault()
            onSave()
          } else if (event.key === "Escape") {
            event.preventDefault()
            isCancelledRef.current = true
            onCancel()
            event.currentTarget.blur()
          }
        }}
      />
    </label>
  )
}

