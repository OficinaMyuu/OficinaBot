export type SessionUser = {
  id: string
  username: string
  globalName: string | null
  avatarUrl: string | null
  guildName: string
  guildIconUrl: string | null
  permissions: string
}

export type SessionResponse = {
  user: SessionUser
  csrfToken: string
}
