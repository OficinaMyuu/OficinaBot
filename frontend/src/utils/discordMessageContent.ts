import type { UserSummary } from "@/types/user"

import { parse as parseTwemoji } from "twemoji-parser"
import {
  getDiscordEmojiUrl,
  getDiscordEntityUrl,
  isDiscordEntityUrl
} from "@/config/discordUrls"

const discordEntityPattern =
  /<@!?(\d+)>|<#(\d+)>|<@&(\d+)>|<(a?):([\w-]+):(\d+)>/g

export function toDiscordMarkdown(
  content: string,
  usersById: Record<string, UserSummary>,
  channelsById: Record<string, string>,
  rolesById: Record<string, string>
): string {
  const withDiscordEntities = content.replace(
    discordEntityPattern,
    (_match, userId, channelId, roleId, animated, emojiName, emojiId) => {
      if (userId) {
        return `[@${usersById[userId]?.display_name ?? userId}](${getDiscordEntityUrl("user", userId)})`
      }
      if (channelId) {
        return `[#${channelsById[channelId] ?? channelId}](${getDiscordEntityUrl("channel", channelId)})`
      }
      if (roleId) {
        return `[@${rolesById[roleId] ?? roleId}](${getDiscordEntityUrl("role", roleId)})`
      }
      return `![${emojiName}](${getDiscordEmojiUrl(emojiId, animated === "a")})`
    }
  )
  return withTwemojiMarkdown(withDiscordEntities)
}

function withTwemojiMarkdown(value: string): string {
  const entities = parseTwemoji(value)
  if (entities.length === 0) return value

  let output = ""
  let offset = 0
  for (const entity of entities) {
    output += value.slice(offset, entity.indices[0])
    output += `![${entity.text}](${entity.url})`
    offset = entity.indices[1]
  }
  return output + value.slice(offset)
}

export function isDiscordMentionUrl(value: string | undefined): boolean {
  return isDiscordEntityUrl(value)
}
