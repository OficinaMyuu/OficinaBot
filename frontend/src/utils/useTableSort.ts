import { useMemo, useState } from 'react'

export type SortDirection = 'asc' | 'desc'

export type TableSortState<T> = {
  sorted: T[]
  sortKey: keyof T | null
  sortDir: SortDirection
  toggle: (key: keyof T) => void
}

export function useTableSort<T>(
  items: T[],
  defaultKey?: keyof T,
  defaultDirection: SortDirection = 'asc'
): TableSortState<T> {
  const [sortKey, setSortKey] = useState<keyof T | null>(defaultKey ?? null)
  const [sortDir, setSortDir] = useState<SortDirection>(defaultDirection)

  const toggle = (key: keyof T) => {
    if (key === sortKey) {
      setSortDir((prev) => (prev === 'asc' ? 'desc' : 'asc'))
    } else {
      setSortKey(key)
      setSortDir('asc')
    }
  }

  const sorted = useMemo(() => {
    if (sortKey === null) return items

    return items.toSorted((a, b) => {
      const aVal = a[sortKey]
      const bVal = b[sortKey]

      if (aVal == null && bVal == null) return 0
      if (aVal == null) return 1
      if (bVal == null) return -1

      let cmp: number
      if (typeof aVal === 'number' && typeof bVal === 'number') {
        cmp = aVal - bVal
      } else if (typeof aVal === 'string' && typeof bVal === 'string') {
        cmp = aVal.localeCompare(bVal)
      } else {
        cmp = aVal < bVal ? -1 : aVal > bVal ? 1 : 0
      }

      return sortDir === 'asc' ? cmp : -cmp
    })
  }, [items, sortKey, sortDir])

  return { sorted, sortKey, sortDir, toggle }
}
