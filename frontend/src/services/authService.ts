import { apiClient, apiUrl, setCsrfToken } from './apiClient'
import type { SessionResponse } from '@/types/session'

const AUTH_BASE = '/auth'

export const authService = {
  async getSession(): Promise<SessionResponse> {
    const session = await apiClient.get<SessionResponse>(`${AUTH_BASE}/me`)
    setCsrfToken(session.csrf_token)
    return session
  },

  login(): void {
    const loginUrl = new URL(apiUrl(`${AUTH_BASE}/discord/login`))
    loginUrl.searchParams.set('return_to', `${window.location.origin}/dashboard`)
    window.location.assign(loginUrl.toString())
  },

  async logout(): Promise<void> {
    await apiClient.post<void>(`${AUTH_BASE}/logout`)
    setCsrfToken(null)
  },
}
