import { fireEvent, render, screen, within } from "@testing-library/react"
import { describe, expect, it } from "vitest"
import { AppTooltip } from "./AppTooltip"

describe("AppTooltip", () => {
  it("shows its label when the wrapped control receives focus", async () => {
    render(
      <AppTooltip label="Log out">
        <button type="button">Sign out</button>
      </AppTooltip>
    )

    fireEvent.focus(screen.getByRole("button", { name: "Sign out" }))

    expect(await screen.findByRole("tooltip")).toHaveTextContent("Log out")
  })

  it("renders an optional image alongside its label", async () => {
    render(
      <AppTooltip
        imageAlt="Myuu profile picture"
        imageSrc="https://example.com/myuu.png"
        label="Myuu"
      >
        <button type="button">Profile</button>
      </AppTooltip>
    )

    fireEvent.focus(screen.getByRole("button", { name: "Profile" }))

    const tooltip = await screen.findByRole("tooltip")

    expect(
      within(tooltip).getByAltText("Myuu profile picture")
    ).toHaveAttribute("src", "https://example.com/myuu.png")
  })
})
