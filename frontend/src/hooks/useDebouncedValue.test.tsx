import { act, render, screen } from "@testing-library/react"
import { afterEach, describe, expect, it, vi } from "vitest"
import { useDebouncedValue } from "./useDebouncedValue"

type DebouncedValueProbeProps = {
  value: string
}

function DebouncedValueProbe({ value }: DebouncedValueProbeProps) {
  const debouncedValue = useDebouncedValue(value, 300)

  return <output data-testid="debounced-value">{debouncedValue}</output>
}

describe("useDebouncedValue", () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it("does not publish input changes until the debounce delay elapses", () => {
    vi.useFakeTimers()
    const { rerender } = render(<DebouncedValueProbe value="" />)

    rerender(<DebouncedValueProbe value="help" />)
    expect(screen.getByTestId("debounced-value")).toHaveTextContent("")

    act(() => {
      vi.advanceTimersByTime(299)
    })
    expect(screen.getByTestId("debounced-value")).toHaveTextContent("")

    act(() => {
      vi.advanceTimersByTime(1)
    })
    expect(screen.getByTestId("debounced-value")).toHaveTextContent("help")
  })
})
