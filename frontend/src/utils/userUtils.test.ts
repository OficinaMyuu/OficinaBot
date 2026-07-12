import { describe, expect, it } from "vitest"
import { getDiscordDisplayName } from "./userUtils"

describe("getDiscordDisplayName", () => {
  it("prefers a Discord global name", () => {
    expect(
      getDiscordDisplayName({
        display_name: "Stored display name",
        global_name: "Global name",
        username: "username"
      })
    ).toBe("Global name")
  })

  it("falls back to the username, then the display name", () => {
    expect(
      getDiscordDisplayName({
        display_name: "Stored display name",
        global_name: null,
        username: "username"
      })
    ).toBe("username")
    expect(
      getDiscordDisplayName({
        display_name: "Stored display name",
        global_name: null,
        username: null
      })
    ).toBe("Stored display name")
  })
})
