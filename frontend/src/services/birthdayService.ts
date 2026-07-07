import { apiClient } from './apiClient'
import type { Birthday, BirthdayPayload, BirthdayQuery } from '@/types/birthday'

const BIRTHDAYS_PATH = '/dashboard/api/birthdays'

type BirthdayListResponse = {
  birthdays: Birthday[]
}

export const birthdayService = {
  async list(query: BirthdayQuery): Promise<Birthday[]> {
    const params = new URLSearchParams()
    if (query.search.trim()) {
      params.set('search', query.search.trim())
    }
    if (query.month !== 'all') {
      params.set('month', query.month)
    }

    const suffix = params.size > 0 ? `?${params.toString()}` : ''
    const response = await apiClient.get<BirthdayListResponse>(`${BIRTHDAYS_PATH}${suffix}`)
    return response.birthdays
  },

  create(payload: BirthdayPayload): Promise<Birthday> {
    return apiClient.post<Birthday>(BIRTHDAYS_PATH, payload)
  },

  update(userId: string, payload: BirthdayPayload): Promise<Birthday> {
    return apiClient.put<Birthday>(`${BIRTHDAYS_PATH}/${userId}`, payload)
  },

  delete(userId: string): Promise<void> {
    return apiClient.delete<void>(`${BIRTHDAYS_PATH}/${userId}`)
  },
}
