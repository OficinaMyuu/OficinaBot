import { describe, expect, it } from "vitest"
import { DiscordPermission, DiscordPermissions } from "./DiscordPermissions"

describe("DiscordPermissions", () => {
  it("checks individual, any, and all permission flags", () => {
    const permissions = new DiscordPermissions(
      DiscordPermission.ManageGuild | DiscordPermission.ManageChannels,
    )

    expect(permissions.isAdmin()).toBe(false)
    expect(permissions.isServerManager()).toBe(true)
    expect(permissions.has(DiscordPermission.ManageChannels)).toBe(true)
    expect(permissions.hasAny(DiscordPermission.BanMembers, DiscordPermission.ManageGuild)).toBe(true)
    expect(permissions.hasAll(DiscordPermission.ManageGuild, DiscordPermission.ManageChannels)).toBe(true)
    expect(permissions.hasAll(DiscordPermission.ManageGuild, DiscordPermission.BanMembers)).toBe(false)
  })

  it("gives administrators effective access to every permission", () => {
    const permissions = new DiscordPermissions(DiscordPermission.Administrator)

    expect(permissions.isAdmin()).toBe(true)
    expect(permissions.isServerManager()).toBe(true)
    expect(permissions.has(DiscordPermission.BypassSlowmode)).toBe(true)
  })

  it("preserves high permission bits from Discord's serialized value", () => {
    const permissions = new DiscordPermissions("4503599627370496")

    expect(permissions.has(DiscordPermission.BypassSlowmode)).toBe(true)
    expect(permissions.toString()).toBe("4503599627370496")
  })

  it("rejects malformed and negative permission values", () => {
    expect(() => new DiscordPermissions("1.5")).toThrow(TypeError)
    expect(() => new DiscordPermissions(-1n)).toThrow(TypeError)
  })
})
