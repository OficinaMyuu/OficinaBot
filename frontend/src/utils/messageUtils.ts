import type { Message, MessageView } from "@/types/message"
import type { UserSummary } from "@/types/user"

export function messageUserIds(messages: Message[]): string[] {
  return messages
    .flatMap((message) => [
      message.author_id,
      message.deleted_by_id,
      ...[...(message.content?.matchAll(/<@!?(\d+)>/g) ?? [])].map(
        (match) => match[1]
      )
    ])
    .filter(Boolean) as string[]
}

export function toMessageViews(
  messages: Message[],
  usersById: Record<string, UserSummary>,
  getFallbackUser: (userId: string) => UserSummary
): MessageView[] {
  return messages.map((message) => ({
    ...message,
    author: usersById[message.author_id] ?? getFallbackUser(message.author_id),
    deleted_by: message.deleted_by_id
      ? (usersById[message.deleted_by_id] ??
        getFallbackUser(message.deleted_by_id))
      : null
  }))
}
