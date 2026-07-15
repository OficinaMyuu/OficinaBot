const DISCORD_CDN_BASE_URL = "https://cdn.discordapp.com"
const DISCORD_LOCAL_BASE_URL = "https://discord.local"

type DiscordEntity = "user" | "channel" | "role"

export function getDiscordStickerUrl(stickerId: string): string {
  return `${DISCORD_CDN_BASE_URL}/stickers/${encodeURIComponent(stickerId)}.png`
}

export function getDiscordEmojiUrl(emojiId: string, animated: boolean): string {
  const extension = animated ? "gif" : "webp"
  return `${DISCORD_CDN_BASE_URL}/emojis/${encodeURIComponent(emojiId)}.${extension}?quality=lossless`
}

export function getDiscordDefaultAvatarUrl(index: number): string {
  return `${DISCORD_CDN_BASE_URL}/embed/avatars/${index}.png`
}

export function getDiscordEntityUrl(entity: DiscordEntity, id: string): string {
  return `${DISCORD_LOCAL_BASE_URL}/${entity}/${encodeURIComponent(id)}`
}

export function isDiscordEntityUrl(value: string | undefined): boolean {
  if (!value) return false

  try {
    const url = new URL(value)
    return (
      url.origin === DISCORD_LOCAL_BASE_URL &&
      /^\/(user|channel|role)\/\d+$/.test(url.pathname)
    )
  } catch {
    return false
  }
}
