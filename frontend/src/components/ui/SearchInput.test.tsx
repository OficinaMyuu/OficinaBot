import { fireEvent, render, screen } from "@testing-library/react"
import { describe, expect, it, vi } from "vitest"
import { SearchInput } from "./SearchInput"

describe("SearchInput", () => {
  it("renders a muted custom clear control instead of the browser search control", () => {
    const onClear = vi.fn()

    render(
      <SearchInput
        aria-label="Search birthdays"
        clearLabel="Clear birthday search"
        onChange={vi.fn()}
        onClear={onClear}
        value="Myuu"
      />
    )

    fireEvent.click(
      screen.getByRole("button", { name: "Clear birthday search" })
    )

    expect(onClear).toHaveBeenCalledOnce()
    expect(screen.getByRole("textbox")).toHaveAttribute("type", "text")
  })
})
