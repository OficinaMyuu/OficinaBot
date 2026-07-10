export type Birthday = {
  user_id: string
  name: string
  birthday: string
  zone_hours: number
  created_at: string
  updated_at: string
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
