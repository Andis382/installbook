import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import {
  centsToInput,
  formatDate,
  formatDateTime,
  formatDay,
  formatMoney,
  formatMonth,
  formatMonthShort,
  formatNumber,
  formatPercent,
  formatPhone,
  formatRelative,
  formatStamp,
  initials,
  parseMoney,
  setFormatLocale,
  setFormatTimeZone,
} from '../format'

describe('format helpers', () => {
  it('groups Albanian mobile numbers', () => {
    expect(formatPhone('355691234567')).toBe('+355 69 123 4567')
  })

  it('reads money the way people type it', () => {
    expect(parseMoney('12,50')).toBe(1250)
    expect(parseMoney('12.5')).toBe(1250)
    expect(parseMoney('')).toBeNull()
    expect(parseMoney('abc')).toBeNull()
  })

  it('round-trips cents to an input value', () => {
    expect(centsToInput(1250)).toBe('12.50')
    expect(centsToInput(1200)).toBe('12')
  })

  it('makes initials', () => {
    expect(initials('Arben Hoxha')).toBe('AH')
    expect(initials('Mira')).toBe('M')
  })
})

describe('Albanian formatting without ICU data for "sq"', () => {
  beforeEach(() => {
    setFormatLocale('sq')
    setFormatTimeZone('Europe/Tirane')
  })
  afterEach(() => {
    setFormatLocale('en')
    setFormatTimeZone(undefined)
  })

  it('writes dates with Albanian month and day names', () => {
    expect(formatDate('2026-10-12')).toBe('12 tet 2026')
    expect(formatDate('2026-10-12', 'short')).toBe('12 tet')
    expect(formatDate('2026-10-12', 'long')).toBe('e hënë, 12 tetor 2026')
    expect(formatMonth('2026-09-01')).toBe('shtator 2026')
    expect(formatMonthShort('2026-12-01')).toBe('dhj')
  })

  it('reads instants in the business time zone', () => {
    // 22:30 UTC on 30 Sep is already 1 Oct, 00:30 in Tirana (summer time)
    expect(formatDateTime('2026-09-30T22:30:00Z')).toBe('1 tet, 00:30')
    expect(formatStamp('2025-09-14T17:42:00Z', new Date('2026-09-23T10:00:00Z'))).toBe(
      '14 sht 2025, 19:42',
    )
  })

  it('says how long ago in Albanian', () => {
    const now = new Date('2026-09-23T10:00:00Z')
    expect(formatRelative('2026-09-20T10:00:00Z', now)).toBe('para 3 ditësh')
    expect(formatRelative('2026-09-22T10:00:00Z', now)).toBe('dje')
    expect(formatRelative('2026-10-07T10:00:00Z', now)).toBe('pas 2 javësh')
    expect(formatRelative('2026-07-25T10:00:00Z', now)).toBe('para 2 muajsh')
  })

  it('groups money and numbers the Albanian way', () => {
    expect(formatMoney(148500)).toBe('1 485 €')
    expect(formatMoney(1250)).toBe('12,50 €')
    expect(formatNumber(1234.5)).toBe('1 234,5')
    expect(formatPercent(0.44)).toBe('44%')
  })
})

describe('dates relative to this year', () => {
  it('drops the year only for this year', () => {
    const now = new Date('2026-09-23T10:00:00Z')
    expect(formatDay('2026-05-18', now)).toBe('18 May')
    expect(formatDay('2025-05-18', now)).toBe('18 May 2025')
  })
})
