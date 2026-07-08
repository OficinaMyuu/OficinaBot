import { describe, expect, it } from 'vitest'
import { resolveApiUrl } from './apiClient'

describe('resolveApiUrl', () => {
  it('resolves API paths against the configured backend origin', () => {
    expect(resolveApiUrl('https://api.oficinamyuu.com.br/', '/birthdays')).toBe('https://api.oficinamyuu.com.br/birthdays')
  })

  it('leaves absolute URLs untouched', () => {
    expect(resolveApiUrl('http://localhost:8080', 'https://api.oficinamyuu.com.br/auth/me')).toBe(
      'https://api.oficinamyuu.com.br/auth/me',
    )
  })
})
