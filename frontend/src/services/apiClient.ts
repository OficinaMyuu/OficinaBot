export type ApiError = {
  message: string
  status?: number
}

let csrfToken: string | null = null
const apiBaseUrl = normalizeBaseUrl(
  import.meta.env.VITE_API_BASE_URL || "https://api.oficinamyuu.com.br"
)

export function setCsrfToken(token: string | null): void {
  csrfToken = token
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  const hasBody = init.body !== undefined

  if (hasBody && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json")
  }
  if (csrfToken && isMutating(init.method)) {
    headers.set("X-CSRF-Token", csrfToken)
  }

  const response = await fetch(apiUrl(path), {
    ...init,
    credentials: "include",
    headers
  })

  if (!response.ok) {
    throw await toApiError(response)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

export function apiUrl(path: string): string {
  return resolveApiUrl(apiBaseUrl, path)
}

export function resolveApiUrl(baseUrl: string, path: string): string {
  if (/^https?:\/\//i.test(path)) {
    return path
  }
  return `${normalizeBaseUrl(baseUrl)}${path.startsWith("/") ? path : `/${path}`}`
}

function normalizeBaseUrl(value: string): string {
  return value.replace(/\/+$/, "")
}

function isMutating(method?: string): boolean {
  const normalized = method?.toUpperCase() ?? "GET"
  return (
    normalized === "POST" ||
    normalized === "PUT" ||
    normalized === "PATCH" ||
    normalized === "DELETE"
  )
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const body = (await response.json()) as {
      message?: string
      error?: string
      status?: number
    }
    return {
      message: body.message ?? body.error ?? response.statusText,
      status: body.status ?? response.status
    }
  } catch {
    return {
      message: response.statusText || "Unexpected request failure",
      status: response.status
    }
  }
}

export const apiClient = {
  get<T>(path: string): Promise<T> {
    return request<T>(path, { method: "GET" })
  },

  post<T>(path: string, body?: unknown): Promise<T> {
    return request<T>(path, {
      method: "POST",
      body: body === undefined ? undefined : JSON.stringify(body)
    })
  },

  put<T>(path: string, body?: unknown): Promise<T> {
    return request<T>(path, {
      method: "PUT",
      body: body === undefined ? undefined : JSON.stringify(body)
    })
  },

  delete<T>(path: string): Promise<T> {
    return request<T>(path, { method: "DELETE" })
  }
}
