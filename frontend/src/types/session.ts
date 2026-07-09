export type SessionUser = {
  id: string
  username: string
  global_name: string | null
  avatar_url: string | null
  guild_name: string
  guild_icon_url: string | null
  permissions: string
}

export type SessionResponse = {
  user: SessionUser
  csrf_token: string
}
