import { apiClient } from "./apiClient"

export type GuildDirectory = {
  channels: Array<{ id: string; name: string }>
  roles: Array<{ id: string; name: string }>
}

export const guildDirectoryService = {
  get: (): Promise<GuildDirectory> => apiClient.get("/discord/guild-directory")
}
