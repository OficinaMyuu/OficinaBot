import { create } from "zustand"
import { guildDirectoryService } from "@/services/guildDirectoryService"

type GuildDirectoryState = {
  channelsById: Record<string, string>
  rolesById: Record<string, string>
  loaded: boolean
  loading: boolean
  load: () => Promise<void>
  reset: () => void
}

export const useGuildDirectoryStore = create<GuildDirectoryState>(
  (set, get) => ({
    channelsById: {},
    rolesById: {},
    loaded: false,
    loading: false,
    async load() {
      if (get().loaded || get().loading) return
      set({ loading: true })
      try {
        const directory = await guildDirectoryService.get()
        set({
          channelsById: Object.fromEntries(
            directory.channels.map((channel) => [channel.id, channel.name])
          ),
          rolesById: Object.fromEntries(
            directory.roles.map((role) => [role.id, role.name])
          ),
          loaded: true,
          loading: false
        })
      } catch {
        set({ loading: false })
      }
    },
    reset: () =>
      set({ channelsById: {}, rolesById: {}, loaded: false, loading: false })
  })
)
