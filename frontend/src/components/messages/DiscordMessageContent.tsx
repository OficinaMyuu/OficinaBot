import type { UserSummary } from "@/types/user"

import ReactMarkdown from "react-markdown"
import remarkGfm from "remark-gfm"
import clsx from "clsx"

import { useGuildDirectoryStore } from "@/stores/useGuildDirectoryStore"
import { isEmojiOnlyMessage } from "./messageContent"
import {
  isDiscordMentionUrl,
  toDiscordMarkdown
} from "@/utils/discordMessageContent"

import styles from "./DiscordMessageContent.module.css"

type DiscordMessageContentProps = {
  content: string
  usersById: Record<string, UserSummary>
}

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
          const mention = isDiscordMentionUrl(href)
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
      {toDiscordMarkdown(content, usersById, channelsById, rolesById)}
    </ReactMarkdown>
  )
}
