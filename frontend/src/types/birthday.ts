export type Birthday = {
  userId: string
  name: string
  birthday: string
  zoneHours: number
  createdAt: number
  updatedAt: number
}

export type BirthdayPayload = {
  userId: string
  name: string
  birthday: string
  zoneHours: number
}

export type BirthdayQuery = {
  search: string
  month: string
}
