import type { MessageView } from "@/types/message"

import { useTranslation } from "react-i18next"
import { FiPaperclip } from "react-icons/fi"

import styles from "./ReplyPreview.module.css"

type ReplyPreviewProps = {
  message: MessageView | null
  referencedMessageId: string | null
  onSelect?: (messageId: string) => void
}

export function ReplyPreview({
  message,
  referencedMessageId,
  onSelect
}: ReplyPreviewProps) {
  const { t } = useTranslation()
  const preview = message?.content?.trim()
  const attachmentOnly = Boolean(message?.sticker_id && !preview)
  const previewLabel = attachmentOnly
    ? t("messages.viewAttachment")
    : preview || t("messages.noTextContent")

  const content = (
    <>
      <span className={styles.spine} aria-hidden="true" />
      {message ? (
        <>
          <strong className={styles.authorName}>
            {message.author.display_name}
          </strong>
          {attachmentOnly ? (
            <span className={styles.attachment}>
              <FiPaperclip aria-hidden="true" />
              <span>{previewLabel}</span>
            </span>
          ) : (
            <span className={styles.preview}>{previewLabel}</span>
          )}
        </>
      ) : (
        <span className={styles.unavailable}>{t("messages.unavailable")}</span>
      )}
    </>
  )

  return onSelect && referencedMessageId ? (
    <button
      className={styles.reply}
      type="button"
      aria-label={`${message?.author.display_name ?? ""} ${message ? previewLabel : t("messages.unavailable")}`.trim()}
      onClick={() => onSelect(referencedMessageId)}
    >
      {content}
    </button>
  ) : (
    <div className={styles.reply}>{content}</div>
  )
}
