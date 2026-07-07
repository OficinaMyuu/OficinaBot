import { apiClient, setCsrfToken } from './apiClient'
import type { SessionResponse } from '@/types/session'

const AUTH_BASE = '/dashboard/api/auth'

export const authService = {
  async getSession(): Promise<SessionResponse> {
    const session = await apiClient.get<SessionResponse>(`${AUTH_BASE}/me`)
    setCsrfToken(session.csrfToken)
    return session
  },

  login(): void {
    window.location.assign('/dashboard/auth/discord/login')
  },

  async logout(): Promise<void> {
    await apiClient.post<void>(`${AUTH_BASE}/logout`)
    setCsrfToken(null)
  },
}
