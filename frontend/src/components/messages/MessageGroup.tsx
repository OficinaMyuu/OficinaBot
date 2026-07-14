import type { MessageView } from "@/types/message"
import type { UserSummary } from "@/types/user"

import { MessageItem } from "./MessageItem"

import styles from "./MessageGroup.module.css"

type MessageGroupProps = {
  channelId: string
  messages: MessageView[]
  messagesById: Map<string, MessageView>
  usersById: Record<string, UserSummary>
  onMessageReferenceSelect?: (messageId: string) => void
}

export function MessageGroup({
  channelId,
  messages,
  messagesById,
  usersById,
  onMessageReferenceSelect
}: MessageGroupProps) {
  return (
    <li className={styles.groupItem}>
      <div className={styles.messageGroup}>
        <ol className={styles.messageList}>
          {messages.map((message, index) => (
            <MessageItem
              key={message.message_id}
              channelId={channelId}
              message={message}
              usersById={usersById}
              referencedMessage={
                message.message_reference_id
                  ? (messagesById.get(message.message_reference_id) ?? null)
                  : null
              }
              grouped={index > 0}
              onMessageReferenceSelect={onMessageReferenceSelect}
            />
          ))}
        </ol>
      </div>
    </li>
  )
}
