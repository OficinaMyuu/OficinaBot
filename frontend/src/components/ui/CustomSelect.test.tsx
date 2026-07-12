import { CustomSelect } from "./CustomSelect"
import { fireEvent, render, screen, waitFor } from "@testing-library/react"
import { describe, expect, it, vi } from "vitest"

describe("CustomSelect", () => {
  it("selects an option without a native select element", () => {
    const onValueChange = vi.fn()

    render(
      <CustomSelect
        ariaLabel="Ticket status"
        value="all"
        onValueChange={onValueChange}
        options={[
          { value: "all", label: "All tickets" },
          { value: "open", label: "Open" }
        ]}
      />
    )

    fireEvent.click(screen.getByRole("combobox", { name: "Ticket status" }))
    fireEvent.click(screen.getByRole("option", { name: "Open" }))

    expect(onValueChange).toHaveBeenCalledWith("open")
    expect(document.querySelector("select")).toBeNull()
  })

  it("filters options and honors a requested menu height", () => {
    render(
      <CustomSelect
        ariaLabel="Ticket status"
        menuHeight={180}
        searchable
        searchPlaceholder="Search ticket statuses"
        value="all"
        onValueChange={vi.fn()}
        options={[
          { value: "all", label: "All tickets" },
          { value: "open", label: "Open tickets" },
          { value: "closed", label: "Closed tickets" }
        ]}
      />
    )

    fireEvent.click(screen.getByRole("combobox", { name: "Ticket status" }))
    fireEvent.change(screen.getByPlaceholderText("Search ticket statuses"), {
      target: { value: "closed" }
    })

    expect(screen.getByRole("option", { name: "Closed tickets" })).toBeVisible()
    expect(
      screen.queryByRole("option", { name: "Open tickets" })
    ).not.toBeInTheDocument()
    expect(screen.getByTestId("custom-select-content")).toHaveStyle(
      "--custom-select-menu-height: 180px"
    )
  })

  it("keeps the search field focused while filtering and supports clearing it", async () => {
    render(
      <CustomSelect
        ariaLabel="Ticket status"
        clearSearchLabel="Clear status search"
        searchable
        searchPlaceholder="Search ticket statuses"
        value="all"
        onValueChange={vi.fn()}
        options={[
          { value: "all", label: "All tickets" },
          { value: "open", label: "Open tickets" },
          { value: "closed", label: "Closed tickets" }
        ]}
      />
    )

    fireEvent.click(screen.getByRole("combobox", { name: "Ticket status" }))
    const searchInput = await screen.findByPlaceholderText(
      "Search ticket statuses"
    )
    fireEvent.change(searchInput, { target: { value: "closed" } })

    await waitFor(() => expect(searchInput).toHaveFocus())
    fireEvent.click(screen.getByRole("button", { name: "Clear status search" }))

    expect(searchInput).toHaveValue("")
  })

  it("highlights options when they are hovered", async () => {
    render(
      <CustomSelect
        ariaLabel="Ticket status"
        searchable
        value="all"
        onValueChange={vi.fn()}
        options={[
          { value: "all", label: "All tickets" },
          { value: "open", label: "Open tickets" }
        ]}
      />
    )

    fireEvent.click(screen.getByRole("combobox", { name: "Ticket status" }))
    const option = await screen.findByRole("option", {
      name: "Open tickets"
    })

    fireEvent.pointerMove(option, { pointerType: "mouse" })

    await waitFor(() => expect(option).toHaveAttribute("data-highlighted"))
  })
})
