import type { UserSummary } from "@/types/user"

import styles from "./TicketUser.module.css"

export function TicketUser({ user }: { user: UserSummary }) {
  return (
    <span className={styles.userValue}>
      <img src={user.avatar_url} alt="" draggable={false} />
      {user.display_name}
    </span>
  )
}
