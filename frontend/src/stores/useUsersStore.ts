import { create } from "zustand"
import { userService } from "@/services/userService"
import type { UserSummary } from "@/types/user"

type UsersState = {
  usersById: Record<string, UserSummary>
  isLoading: boolean
  error: string | null
  fetchUsers: (userIds: string[]) => Promise<void>
  reset: () => void
}

export const useUsersStore = create<UsersState>((set, get) => ({
  usersById: {},
  isLoading: false,
  error: null,

  async fetchUsers(userIds) {
    const missingIds = [...new Set(userIds.filter(Boolean))].filter(
      (id) => !get().usersById[id]
    )
    if (missingIds.length === 0) {
      return
    }

    set({ isLoading: true, error: null })
    try {
      const users = await userService.query(missingIds)
      set((state) => ({
        usersById: {
          ...state.usersById,
          ...Object.fromEntries(users.map((user) => [user.id, user]))
        },
        isLoading: false
      }))
    } catch (error) {
      set({ error: toMessage(error), isLoading: false })
    }
  },

  reset() {
    set({ usersById: {}, isLoading: false, error: null })
  }
}))

export function fallbackUser(userId: string): UserSummary {
  return {
    id: userId,
    username: null,
    global_name: null,
    display_name: userId,
    avatar_hash: null,
    avatar_url: `https://cdn.discordapp.com/embed/avatars/${defaultAvatarIndex(userId)}.png`
  }
}

function defaultAvatarIndex(userId: string): number {
  try {
    return Number((BigInt(userId) >> 22n) % 6n)
  } catch {
    return 0
  }
}

function toMessage(error: unknown): string {
  if (typeof error === "object" && error !== null && "message" in error) {
    return String(error.message)
  }
  if (error instanceof Error) {
    return error.message
  }
  return "Unexpected error"
}
