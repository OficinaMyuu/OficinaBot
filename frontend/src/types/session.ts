import { DiscordPermissions } from "@/domain/discord/DiscordPermissions"

export type SessionUser = {
  id: string
  username: string
  global_name: string | null
  avatar_url: string | null
  guild_name: string
  guild_icon_url: string | null
  permissions: DiscordPermissions
}

export type SessionResponse = {
  user: SessionUser
  csrf_token: string
}

export type SessionUserResponse = Omit<SessionUser, "permissions"> & {
  permissions: string
}

export type SessionResponsePayload = Omit<SessionResponse, "user"> & {
  user: SessionUserResponse
}
