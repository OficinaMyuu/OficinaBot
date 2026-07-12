import type { UserSummary } from "@/types/user"

import { AppTooltip } from "@/components/ui/AppTooltip"
import { formatLocalTimestamp } from "@/utils/timeUtils"
import { getDiscordDisplayName } from "@/utils/userUtils"

import styles from "./ActionCostUpdatedBy.module.css"

type ActionCostUpdatedByProps = {
  updatedAt: string
  updatedBy: UserSummary | null
}

export function ActionCostUpdatedBy({
  updatedAt,
  updatedBy
}: ActionCostUpdatedByProps) {
  return (
    <span className={styles.lastUpdated}>
      {updatedBy ? (
        <AppTooltip label={getDiscordDisplayName(updatedBy)}>
          <img
            className={styles.updaterAvatar}
            src={updatedBy.avatar_url}
            alt=""
          />
        </AppTooltip>
      ) : null}
      <time dateTime={updatedAt}>{formatLocalTimestamp(updatedAt)}</time>
    </span>
  )
}
