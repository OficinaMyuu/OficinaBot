import { describe, expect, it } from "vitest"
import type { Message } from "@/types/message"
import type { UserSummary } from "@/types/user"
import { messageUserIds, toMessageViews } from "./messageUtils"

describe("messageUtils", () => {
  it("collects authors, auditors, and mentioned users", () => {
    expect(
      messageUserIds([
        { ...message, deleted_by_id: "99", content: "Hi <@123>" }
      ])
    ).toEqual(["42", "99", "123"])
  })

  it("resolves known and fallback message users", () => {
    const fallback = createUser("fallback")
    const views = toMessageViews(
      [{ ...message, deleted_by_id: "99" }],
      { "42": createUser("author") },
      () => fallback
    )
    expect(views[0].author.username).toBe("author")
    expect(views[0].deleted_by).toBe(fallback)
  })
})

const message: Message = {
  message_id: "1",
  author_id: "42",
  message_reference_id: null,
  content: "Hello",
  sticker_id: null,
  is_edited: false,
  revision_count: 1,
  is_deleted: false,
  deleted_by_id: null,
  created_at: "2023-11-14T22:13:20Z",
  updated_at: "2023-11-14T22:13:20Z"
}

function createUser(username: string): UserSummary {
  return {
    id: username,
    username,
    global_name: username,
    display_name: username,
    avatar_hash: null,
    avatar_url: "https://cdn.discordapp.com/embed/avatars/0.png",
    is_bot: false
  }
}
