export type Birthday = {
  user_id: string
  name: string
  birthday: string
  zone_hours: number
  created_at: number
  updated_at: number
}

export type BirthdayPayload = {
  user_id: string
  name: string
  birthday: string
  zone_hours: number
}

export type BirthdayQuery = {
  search: string
  month: string
}
