import type { MessageView } from "@/types/message"

export function isGroupedMessage(
  previous: MessageView | undefined,
  message: MessageView
): boolean {
  if (
    !previous ||
    message.message_reference_id !== null ||
    previous.author_id !== message.author_id ||
    previous.is_deleted ||
    message.is_deleted
  ) {
    return false
  }
  return (
    new Date(message.created_at).getTime() -
      new Date(previous.created_at).getTime() <=
    5 * 60 * 1000
  )
}
