import { describe, expect, it } from "vitest"
import { isEmojiOnlyMessage } from "./messageContent"

describe("isEmojiOnlyMessage", () => {
  it("accepts unicode and custom emoji separated only by whitespace", () => {
    expect(isEmojiOnlyMessage("😀")).toBe(true)
    expect(isEmojiOnlyMessage("😀  <:party:123456789>\n")).toBe(true)
    expect(isEmojiOnlyMessage("<a:dance:987654321>")).toBe(true)
  })

  it("rejects emoji mixed with message content", () => {
    expect(isEmojiOnlyMessage("hello 😀")).toBe(false)
    expect(isEmojiOnlyMessage("😀!")).toBe(false)
    expect(isEmojiOnlyMessage("plain text")).toBe(false)
  })
})
