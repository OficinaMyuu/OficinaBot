import { create } from "zustand"
import { birthdayService } from "@/services/birthdayService"
import type { Birthday, BirthdayPayload, BirthdayQuery } from "@/types/birthday"

type BirthdaysState = {
  birthdays: Birthday[]
  query: BirthdayQuery
  isLoading: boolean
  isSaving: boolean
  error: string | null
  load: (query: BirthdayQuery) => Promise<void>
  refresh: () => Promise<void>
  createBirthday: (payload: BirthdayPayload) => Promise<Birthday>
  updateBirthday: (payload: BirthdayPayload) => Promise<Birthday>
  deleteBirthday: (birthday: Birthday) => Promise<void>
  reset: () => void
}

const defaultQuery: BirthdayQuery = { search: "", month: "all" }
let birthdayRequestId = 0

export const useBirthdaysStore = create<BirthdaysState>((set, get) => ({
  birthdays: [],
  query: defaultQuery,
  isLoading: false,
  isSaving: false,
  error: null,

  async load(query) {
    const requestId = ++birthdayRequestId
    set({ query, isLoading: true, error: null })
    try {
      const birthdays = await birthdayService.list(query)
      if (requestId === birthdayRequestId) {
        set({ birthdays, isLoading: false })
      }
    } catch (error) {
      if (requestId === birthdayRequestId) {
        set({ error: toMessage(error), isLoading: false })
      }
    }
  },

  refresh() {
    return get().load(get().query)
  },

  async createBirthday(payload) {
    set({ isSaving: true })
    try {
      const birthday = await birthdayService.create(payload)
      await get().refresh()
      return birthday
    } finally {
      set({ isSaving: false })
    }
  },

  async updateBirthday(payload) {
    set({ isSaving: true })
    try {
      const birthday = await birthdayService.update(payload.user_id, payload)
      await get().refresh()
      return birthday
    } finally {
      set({ isSaving: false })
    }
  },

  async deleteBirthday(birthday) {
    set({ isSaving: true })
    try {
      await birthdayService.delete(birthday.user_id)
      await get().refresh()
    } finally {
      set({ isSaving: false })
    }
  },

  reset() {
    birthdayRequestId += 1
    set({
      birthdays: [],
      query: defaultQuery,
      isLoading: false,
      isSaving: false,
      error: null
    })
  }
}))

function toMessage(error: unknown): string {
  if (typeof error === "object" && error !== null && "message" in error) {
    return String(error.message)
  }
  if (error instanceof Error) {
    return error.message
  }
  return "Unexpected error"
}
