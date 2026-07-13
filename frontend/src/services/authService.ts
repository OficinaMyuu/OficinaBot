import { apiClient, apiUrl, setCsrfToken } from "./apiClient"
import { DiscordPermissions } from "@/domain/discord/DiscordPermissions"
import type { SessionResponse, SessionResponsePayload } from "@/types/session"

const AUTH_BASE = "/auth"

export const authService = {
  async getSession(): Promise<SessionResponse> {
    const response = await apiClient.get<SessionResponsePayload>(`${AUTH_BASE}/me`)
    const session: SessionResponse = {
      ...response,
      user: {
        ...response.user,
        permissions: new DiscordPermissions(response.user.permissions),
      },
    }
    setCsrfToken(session.csrf_token)
    return session
  },

  login(): void {
    const loginUrl = new URL(
      apiUrl(`${AUTH_BASE}/discord/login`),
      window.location.origin
    )
    loginUrl.searchParams.set("return_to", dashboardReturnTo())
    window.location.assign(loginUrl.toString())
  },

  async logout(): Promise<void> {
    await apiClient.post<void>(`${AUTH_BASE}/logout`)
    setCsrfToken(null)
  }
}

function dashboardReturnTo(): string {
  const current = new URL(window.location.href)
  current.hash = ""
  if (
    current.pathname.startsWith("/dashboard") &&
    current.pathname !== "/dashboard/login"
  ) {
    return current.toString()
  }
  return `${window.location.origin}/dashboard`
}
