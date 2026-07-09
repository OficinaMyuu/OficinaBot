import { afterEach, describe, expect, it, vi } from 'vitest'
import { ticketService } from './ticketService'

describe('ticketService', () => {
  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('lists tickets with trimmed filters and cursor pagination', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ tickets: [], next_cursor: null }))
    vi.stubGlobal('fetch', fetchMock)

    await ticketService.list({ search: ' help ', status: 'open', limit: 25, cursor: '10:7' })

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/tickets?search=help&status=open&limit=25&cursor=10%3A7',
      expect.objectContaining({ credentials: 'include', method: 'GET' }),
    )
  })

  it('loads ticket messages from the snake_case endpoint', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ ticket: null, messages: [], next_cursor: null }))
    vi.stubGlobal('fetch', fetchMock)

    await ticketService.messages(7, { limit: 50, cursor: '1000:101' })

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/tickets/7/messages?limit=50&cursor=1000%3A101',
      expect.objectContaining({ credentials: 'include', method: 'GET' }),
    )
  })
})

function jsonResponse(body: unknown): Response {
  return new Response(JSON.stringify(body), {
    headers: { 'Content-Type': 'application/json' },
    status: 200,
  })
}
