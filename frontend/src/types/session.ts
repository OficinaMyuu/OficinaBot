export type SessionUser = {
  id: string
  username: string
  globalName: string | null
  avatarUrl: string | null
  guildName: string
  permissions: string
}

export type SessionResponse = {
  user: SessionUser
  csrfToken: string
}
