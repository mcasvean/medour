import { describe, it, expect } from 'vitest'
import { formatDate, formatTime, formatBooked } from '../formatDate'

describe('formatDate', () => {
  it('converts YYYY-MM-DD to DD-MM-YYYY', () => {
    expect(formatDate('2026-08-18')).toBe('18-08-2026')
  })

  it('pads single-digit day and month', () => {
    expect(formatDate('2026-01-05')).toBe('05-01-2026')
  })
})

describe('formatTime', () => {
  it('strips seconds from HH:MM:SS', () => {
    expect(formatTime('10:00:00')).toBe('10:00')
  })

  it('leaves HH:MM unchanged', () => {
    expect(formatTime('09:30')).toBe('09:30')
  })
})

describe('formatBooked', () => {
  it('formats ISO timestamp as DD-MM-YYYY, HH:MM in local time', () => {
    // use a fixed UTC timestamp and check structure, not exact value (locale-dependent)
    const result = formatBooked('2026-08-17T15:46:00Z')
    expect(result).toMatch(/^\d{2}-\d{2}-\d{4}, \d{2}:\d{2}$/)
  })
})
