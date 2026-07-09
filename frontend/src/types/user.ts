export type UserSummary = {
  id: string
  username: string | null
  global_name: string | null
  display_name: string
  avatar_hash: string | null
  avatar_url: string
}

export type UserQueryResponse = {
  users: UserSummary[]
}
