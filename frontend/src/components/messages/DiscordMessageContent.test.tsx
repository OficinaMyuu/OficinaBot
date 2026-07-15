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

  it("renders custom emoji entities through the shared Discord URL builder", () => {
    render(
      <DiscordMessageContent content="Hello <:wave:123456789>" usersById={{}} />
    )

    expect(screen.getByRole("img")).toHaveAttribute(
      "src",
      "https://cdn.discordapp.com/emojis/123456789.webp?quality=lossless"
    )
  })
})
