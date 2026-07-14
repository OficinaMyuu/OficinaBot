import type { UserSummary } from "@/types/user"

import ReactMarkdown from "react-markdown"
import remarkGfm from "remark-gfm"
import { parse as parseTwemoji } from "twemoji-parser"
import { useGuildDirectoryStore } from "@/stores/useGuildDirectoryStore"
import { isEmojiOnlyMessage } from "./messageContent"

import clsx from "clsx"

import styles from "./DiscordMessageContent.module.css"

type DiscordMessageContentProps = {
  content: string
  usersById: Record<string, UserSummary>
}

const discordEntityPattern =
  /<@!?(\d+)>|<#(\d+)>|<@&(\d+)>|<(a?):([\w-]+):(\d+)>/g

export function DiscordMessageContent({
  content,
  usersById
}: DiscordMessageContentProps) {
  const channelsById = useGuildDirectoryStore((state) => state.channelsById)
  const rolesById = useGuildDirectoryStore((state) => state.rolesById)
  const emojiOnly = isEmojiOnlyMessage(content)

  return (
    <ReactMarkdown
      remarkPlugins={[remarkGfm]}
      components={{
        a: ({ href, children }) => {
          const mention = parseMentionURL(href)
          if (mention) {
            return <span className={styles.mention}>{children}</span>
          }
          return <a href={href}>{children}</a>
        },
        img: ({ src, alt }) => (
          <img
            className={clsx(styles.inlineEmoji, emojiOnly && styles.emojiOnly)}
            src={src}
            alt={alt ?? ""}
            draggable={false}
          />
        )
      }}
    >
      {toMarkdown(content, usersById, channelsById, rolesById)}
    </ReactMarkdown>
  )
}

function toMarkdown(
  content: string,
  usersById: Record<string, UserSummary>,
  channelsById: Record<string, string>,
  rolesById: Record<string, string>
): string {
  const withDiscordEntities = content.replace(
    discordEntityPattern,
    (_match, userID, channelID, roleID, animated, emojiName, emojiID) => {
      if (userID)
        return `[@${usersById[userID]?.display_name ?? userID}](https://discord.local/user/${userID})`
      if (channelID)
        return `[#${channelsById[channelID] ?? channelID}](https://discord.local/channel/${channelID})`
      if (roleID)
        return `[@${rolesById[roleID] ?? roleID}](https://discord.local/role/${roleID})`
      const extension = animated === "a" ? "gif" : "webp"
      return `![${emojiName}](https://cdn.discordapp.com/emojis/${emojiID}.${extension}?quality=lossless)`
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

function parseMentionURL(value: string | undefined): boolean {
  return Boolean(
    value && /^https:\/\/discord\.local\/(user|channel|role)\/\d+$/.test(value)
  )
}
