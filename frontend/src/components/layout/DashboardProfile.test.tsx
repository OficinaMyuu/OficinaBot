import { render, screen } from "@testing-library/react"
import { describe, expect, it, vi } from "vitest"
import { DiscordPermission, DiscordPermissions } from "@/domain/discord/DiscordPermissions"
import { DashboardProfile } from "./DashboardProfile"
import "@/services/i18n"

describe("DashboardProfile", () => {
  it("identifies an administrator in English", () => {
    render(
      <DashboardProfile
        onLogout={vi.fn()}
        user={createUser(DiscordPermission.Administrator)}
      />,
    )

    expect(screen.getByText("Administrator")).toBeInTheDocument()
  })

  it("identifies a non-administrator with Manage Server in English", () => {
    render(
      <DashboardProfile onLogout={vi.fn()} user={createUser(DiscordPermission.ManageGuild)} />,
    )

    expect(screen.getByText("Server Manager")).toBeInTheDocument()
  })
})

function createUser(permissions: bigint) {
  return {
    id: "42",
    username: "myuu",
    global_name: "Myuu",
    avatar_url: null,
    guild_name: "Oficina",
    guild_icon_url: null,
    permissions: new DiscordPermissions(permissions),
  }
}
