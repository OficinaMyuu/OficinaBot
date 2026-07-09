import { describe, expect, it } from 'vitest'
import { epochToDate } from './time'

describe('epochToDate', () => {
  it('accepts epoch seconds and milliseconds', () => {
    expect(epochToDate(1_700_000_000).getTime()).toBe(1_700_000_000_000)
    expect(epochToDate(1_700_000_000_123).getTime()).toBe(1_700_000_000_123)
  })
})
