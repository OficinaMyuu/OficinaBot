import { apiClient } from "./apiClient"
import type { UserQueryResponse, UserSummary } from "@/types/user"

const USERS_PATH = "/users"

export const userService = {
  async query(userIds: string[]): Promise<UserSummary[]> {
    const ids = [...new Set(userIds.filter(Boolean))]
    if (ids.length === 0) {
      return []
    }

    const response = await apiClient.post<UserQueryResponse>(
      `${USERS_PATH}/query`,
      { user_ids: ids }
    )
    return response.users
  }
}
