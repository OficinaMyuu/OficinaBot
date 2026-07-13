import { render, screen } from "@testing-library/react"
import type { ReactNode } from "react"
import { describe, expect, it, vi } from "vitest"
import { DiscordPermissions } from "@/domain/discord/DiscordPermissions"
import { DashboardLayout } from "./DashboardLayout"
import "@/services/i18n"

vi.mock("@/contexts/SessionContext", () => ({
  useSession: () => ({
    user: {
      id: "42",
      username: "myuu",
      global_name: "Myuu",
      avatar_url: null,
      guild_name: "Oficina",
      guild_icon_url: null,
      permissions: new DiscordPermissions("32")
    },
    logout: vi.fn()
  })
}))

vi.mock("@tanstack/react-router", () => ({
  Link: ({ children, to }: { children: ReactNode; to: string }) => (
    <a href={to}>{children}</a>
  )
}))

vi.mock("react-resizable-panels", () => ({
  Group: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  Panel: ({ children }: { children: ReactNode }) => <div>{children}</div>,
  Separator: () => <div />
}))

describe("DashboardLayout", () => {
  it("groups dashboard links by operational category", () => {
    render(
      <DashboardLayout title="Dashboard">
        <p>Content</p>
      </DashboardLayout>
    )

    expect(screen.getByText("MISC")).toBeInTheDocument()
    expect(screen.getByText("MODERAÇÃO")).toBeInTheDocument()
    expect(screen.getByText(/ECONOMY|ECONOMIA/)).toBeInTheDocument()
    expect(screen.getByRole("link", { name: /aniversários/i })).toHaveAttribute(
      "href",
      "/dashboard/birthdays"
    )
    expect(screen.getByRole("link", { name: /tickets/i })).toHaveAttribute(
      "href",
      "/dashboard/tickets"
    )
    expect(
      screen.getByRole("link", { name: /preços|action costs|custos/i })
    ).toHaveAttribute("href", "/dashboard/economy/action-costs")
  })
})
