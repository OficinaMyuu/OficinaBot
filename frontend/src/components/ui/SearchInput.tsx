import type { InputHTMLAttributes } from "react"
import { FiSearch, FiX } from "react-icons/fi"
import styles from "./SearchInput.module.css"

type SearchInputProps = Omit<InputHTMLAttributes<HTMLInputElement>, "type"> & {
  clearLabel?: string
  onClear?: () => void
}

export function SearchInput({
  className,
  clearLabel = "Clear search",
  onClear,
  value,
  ...props
}: SearchInputProps) {
  const hasValue = typeof value === "string" && value.length > 0

  return (
    <div className={[styles.root, className].filter(Boolean).join(" ")}>
      <FiSearch aria-hidden="true" />
      <input type="text" value={value} {...props} />
      {hasValue && onClear ? (
        <button
          className={styles.clearButton}
          type="button"
          aria-label={clearLabel}
          onMouseDown={(event) => event.preventDefault()}
          onClick={onClear}
        >
          <FiX aria-hidden="true" />
        </button>
      ) : null}
    </div>
  )
}
