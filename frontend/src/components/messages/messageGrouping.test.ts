import { describe, expect, it } from "vitest"
import type { MessageView } from "@/types/message"
import { createMessageGroups } from "./messageGrouping"

describe("createMessageGroups", () => {
  it("groups consecutive messages while replies begin a new group", () => {
    const groups = createMessageGroups([
      message({ message_id: "100" }),
      message({ message_id: "101", created_at: "2023-11-14T22:14:20Z" }),
      message({
        message_id: "102",
        message_reference_id: "100",
        created_at: "2023-11-14T22:15:20Z"
      }),
      message({ message_id: "103", created_at: "2023-11-14T22:16:20Z" })
    ])

    expect(groups.map((group) => group.map((item) => item.message_id))).toEqual(
      [
        ["100", "101"],
        ["102", "103"]
      ]
    )
  })

  it("splits groups across author, deletion, and time boundaries", () => {
    const groups = createMessageGroups([
      message({ message_id: "100" }),
      message({ message_id: "101", author_id: "84" }),
      message({ message_id: "102", is_deleted: true }),
      message({ message_id: "103", created_at: "2023-11-14T22:20:20Z" })
    ])

    expect(groups).toHaveLength(4)
  })
})

function message(overrides: Partial<MessageView>): MessageView {
  return {
    message_id: "100",
    author_id: "42",
    author: {
      id: "42",
      username: "myuu",
      global_name: "Myuu",
      display_name: "Myuu",
      avatar_hash: null,
      avatar_url: "/avatar.png",
      is_bot: false
    },
    message_reference_id: null,
    content: "Hello",
    sticker_id: null,
    is_edited: false,
    revision_count: 1,
    is_deleted: false,
    deleted_by_id: null,
    deleted_by: null,
    created_at: "2023-11-14T22:13:20Z",
    updated_at: "2023-11-14T22:13:20Z",
    ...overrides
  }
}
