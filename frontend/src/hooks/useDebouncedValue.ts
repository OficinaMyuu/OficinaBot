import { useEffect, useState } from "react"

import debounce from "lodash-es/debounce"

export function useDebouncedValue<T>(value: T, delay = 300): T {
  const [debouncedValue, setDebouncedValue] = useState(value)

  useEffect(() => {
    const updateValue = debounce(setDebouncedValue, delay)
    updateValue(value)

    return () => updateValue.cancel()
  }, [delay, value])

  return debouncedValue
}
