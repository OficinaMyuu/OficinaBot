import { parse as parseTwemoji } from "twemoji-parser"

const customEmojiPattern = /<a?:[\w-]+:\d+>/g

export function isEmojiOnlyMessage(content: string): boolean {
  let emojiCount = 0
  const withoutCustomEmoji = content.replace(customEmojiPattern, () => {
    emojiCount += 1
    return ""
  })
  const unicodeEmoji = parseTwemoji(withoutCustomEmoji)
  emojiCount += unicodeEmoji.length

  if (emojiCount === 0) return false

  let remaining = withoutCustomEmoji
  for (let index = unicodeEmoji.length - 1; index >= 0; index -= 1) {
    const [start, end] = unicodeEmoji[index].indices
    remaining = remaining.slice(0, start) + remaining.slice(end)
  }

  return remaining.trim().length === 0
}
