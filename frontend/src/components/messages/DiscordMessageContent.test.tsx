import { render, screen } from "@testing-library/react"
import { describe, expect, it } from "vitest"
import { DiscordMessageContent } from "./DiscordMessageContent"
import styles from "./DiscordMessageContent.module.css"

describe("DiscordMessageContent", () => {
  it("enlarges emoji-only messages but keeps emoji inline with text", () => {
    const { rerender } = render(
      <DiscordMessageContent content="😀" usersById={{}} />
    )

    expect(screen.getByRole("img")).toHaveClass(styles.emojiOnly)

    rerender(<DiscordMessageContent content="hello 😀" usersById={{}} />)

    expect(screen.getByRole("img")).not.toHaveClass(styles.emojiOnly)
  })
})
