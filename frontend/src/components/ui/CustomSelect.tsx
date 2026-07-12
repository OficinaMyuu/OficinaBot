import type { CSSProperties } from "react"
import { useEffect, useMemo, useRef, useState } from "react"

import * as Select from "@radix-ui/react-select"

import { FiCheck, FiChevronDown, FiSearch, FiX } from "react-icons/fi"

import styles from "./CustomSelect.module.css"

export type CustomSelectOption<T extends string> = {
  value: T
  label: string
}

type CustomSelectProps<T extends string> = {
  ariaLabel: string
  className?: string
  clearSearchLabel?: string
  disabled?: boolean
  emptyMessage?: string
  menuHeight?: number | string
  onValueChange: (value: T) => void
  options: readonly CustomSelectOption<T>[]
  searchable?: boolean
  searchPlaceholder?: string
  value: T
}

export function CustomSelect<T extends string>({
  ariaLabel,
  className,
  clearSearchLabel = "Clear search",
  disabled = false,
  emptyMessage = "No options found.",
  menuHeight,
  onValueChange,
  options,
  searchable = false,
  searchPlaceholder = "Search options...",
  value
}: CustomSelectProps<T>) {
  const [isOpen, setIsOpen] = useState(false)
  const [searchTerm, setSearchTerm] = useState("")
  const searchInputRef = useRef<HTMLInputElement>(null)
  const selectedOption = options.find((option) => option.value === value)
  const filteredOptions = useMemo(() => {
    const normalizedSearchTerm = searchTerm.trim().toLocaleLowerCase()

    if (!searchable || !normalizedSearchTerm) {
      return options
    }

    return options.filter((option) =>
      option.label.toLocaleLowerCase().includes(normalizedSearchTerm)
    )
  }, [options, searchable, searchTerm])
  const contentClassName = [
    styles.content,
    menuHeight === undefined ? undefined : styles.fixedHeight
  ]
    .filter(Boolean)
    .join(" ")
  const contentStyle =
    menuHeight === undefined
      ? undefined
      : ({
          "--custom-select-menu-height": toCssSize(menuHeight)
        } as CSSProperties)

  useEffect(() => {
    if (!isOpen || !searchable) {
      return
    }

    const focusTimer = window.setTimeout(() => {
      searchInputRef.current?.focus({ preventScroll: true })
    })

    return () => window.clearTimeout(focusTimer)
  }, [isOpen, searchable, searchTerm])

  const handleOpenChange = (open: boolean) => {
    setIsOpen(open)

    if (!open) {
      setSearchTerm("")
    }
  }

  return (
    <Select.Root
      value={value}
      disabled={disabled}
      open={isOpen}
      onOpenChange={handleOpenChange}
      onValueChange={(nextValue) => onValueChange(nextValue as T)}
    >
      <Select.Trigger
        className={[styles.trigger, className].filter(Boolean).join(" ")}
        aria-label={ariaLabel}
      >
        <Select.Value>{selectedOption?.label}</Select.Value>
        <Select.Icon className={styles.icon}>
          <FiChevronDown aria-hidden="true" />
        </Select.Icon>
      </Select.Trigger>

      <Select.Portal>
        <Select.Content
          className={contentClassName}
          collisionPadding={12}
          data-testid="custom-select-content"
          position="popper"
          sideOffset={6}
          style={contentStyle}
        >
          {searchable ? (
            <div className={styles.searchWrapper}>
              <FiSearch className={styles.searchIcon} aria-hidden="true" />
              <input
                ref={searchInputRef}
                className={styles.searchInput}
                placeholder={searchPlaceholder}
                type="text"
                value={searchTerm}
                onChange={(event) => setSearchTerm(event.target.value)}
                onKeyDown={(event) => event.stopPropagation()}
              />
              {searchTerm ? (
                <button
                  className={styles.clearButton}
                  type="button"
                  aria-label={clearSearchLabel}
                  onMouseDown={(event) => event.preventDefault()}
                  onClick={() => setSearchTerm("")}
                >
                  <FiX aria-hidden="true" />
                </button>
              ) : null}
            </div>
          ) : null}
          <Select.Viewport className={styles.viewport}>
            {filteredOptions.map((option) => (
              <Select.Item
                className={styles.item}
                key={option.value}
                value={option.value}
              >
                <Select.ItemText>{option.label}</Select.ItemText>
                <Select.ItemIndicator className={styles.indicator}>
                  <FiCheck aria-hidden="true" />
                </Select.ItemIndicator>
              </Select.Item>
            ))}
            {filteredOptions.length === 0 ? (
              <p className={styles.empty}>{emptyMessage}</p>
            ) : null}
          </Select.Viewport>
        </Select.Content>
      </Select.Portal>
    </Select.Root>
  )
}

function toCssSize(value: number | string): string {
  return typeof value === "number" ? `${value}px` : value
}
