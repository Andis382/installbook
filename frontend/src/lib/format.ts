/**
 * Locale-aware formatting. English goes through Intl ("en-GB": day first). Albanian is
 * composed here from CLDR's names and patterns, because not every browser ships ICU data
 * for "sq" (headless Chromium, some WebViews) and a date in English on an Albanian screen
 * reads as a bug. Intl still supplies the calendar parts, so time zones stay correct.
 */

let locale = 'en'
let currency = 'EUR'
let timeZone: string | undefined

export function setFormatLocale(next: string) {
  locale = next
}

export function setFormatCurrency(next: string) {
  currency = next
}

export function setFormatTimeZone(next: string | undefined) {
  timeZone = next
}

type Value = string | number | Date | null | undefined

const SQ = {
  months: [
    'janar',
    'shkurt',
    'mars',
    'prill',
    'maj',
    'qershor',
    'korrik',
    'gusht',
    'shtator',
    'tetor',
    'nëntor',
    'dhjetor',
  ],
  monthsShort: [
    'jan',
    'shk',
    'mar',
    'pri',
    'maj',
    'qer',
    'korr',
    'gush',
    'sht',
    'tet',
    'nën',
    'dhj',
  ],
  weekdays: ['e diel', 'e hënë', 'e martë', 'e mërkurë', 'e enjte', 'e premte', 'e shtunë'],
  weekdaysShort: ['Die', 'Hën', 'Mar', 'Mër', 'Enj', 'Pre', 'Sht'],
  /** Ablative forms after "para"/"pas": [singular, plural]. */
  units: {
    second: ['sekonde', 'sekondash'],
    minute: ['minute', 'minutash'],
    hour: ['ore', 'orësh'],
    day: ['dite', 'ditësh'],
    week: ['jave', 'javësh'],
    month: ['muaji', 'muajsh'],
    year: ['viti', 'vitesh'],
  },
}

const NBSP = ' '

function isSq() {
  return locale === 'sq'
}

function isPlainDate(value: unknown): value is string {
  return typeof value === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(value)
}

function toDate(value: string | number | Date): Date {
  if (value instanceof Date) return value
  // Plain dates ("2026-09-23") are calendar days, not instants: read them at local noon.
  if (isPlainDate(value)) return new Date(value + 'T12:00:00')
  return new Date(value)
}

type Parts = {
  year: number
  month: number
  day: number
  weekday: number
  hour: string
  minute: string
}

/** Calendar parts of a moment in the business's time zone (plain dates are taken as they are). */
function parts(value: string | number | Date): Parts {
  const d = toDate(value)
  const zone = isPlainDate(value) ? undefined : timeZone
  const p = Object.fromEntries(
    new Intl.DateTimeFormat('en-GB', {
      timeZone: zone,
      year: 'numeric',
      month: 'numeric',
      day: 'numeric',
      weekday: 'short',
      hour: '2-digit',
      minute: '2-digit',
      hourCycle: 'h23',
    })
      .formatToParts(d)
      .map((x) => [x.type, x.value]),
  )
  const weekdays = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat']
  return {
    year: Number(p.year),
    month: Number(p.month),
    day: Number(p.day),
    weekday: weekdays.indexOf(p.weekday ?? 'Sun'),
    hour: p.hour ?? '00',
    minute: p.minute ?? '00',
  }
}

function tag() {
  return isSq() ? 'sq-AL' : 'en-GB'
}

export function formatDate(value: Value, style: 'short' | 'medium' | 'long' = 'medium') {
  if (value === null || value === undefined || value === '') return '—'
  if (isSq()) {
    const p = parts(value)
    if (style === 'short') return `${p.day} ${SQ.monthsShort[p.month - 1]}`
    if (style === 'long')
      return `${SQ.weekdays[p.weekday]}, ${p.day} ${SQ.months[p.month - 1]} ${p.year}`
    return `${p.day} ${SQ.monthsShort[p.month - 1]} ${p.year}`
  }
  const options: Intl.DateTimeFormatOptions =
    style === 'short'
      ? { day: 'numeric', month: 'short' }
      : style === 'long'
        ? { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }
        : { day: 'numeric', month: 'short', year: 'numeric' }
  return new Intl.DateTimeFormat(tag(), {
    ...options,
    timeZone: isPlainDate(value) ? undefined : timeZone,
  }).format(toDate(value))
}

/** A day without its year when it is this year ("12 Oct"), with it otherwise ("12 Oct 2025"). */
export function formatDay(value: Value, now: Date = new Date()) {
  if (value === null || value === undefined || value === '') return '—'
  return parts(value).year === parts(now).year ? formatDate(value, 'short') : formatDate(value)
}

export function formatDateTime(value: Value) {
  if (value === null || value === undefined || value === '') return '—'
  if (isSq()) {
    const p = parts(value)
    return `${p.day} ${SQ.monthsShort[p.month - 1]}, ${p.hour}:${p.minute}`
  }
  return new Intl.DateTimeFormat(tag(), {
    day: 'numeric',
    month: 'short',
    hour: '2-digit',
    minute: '2-digit',
    timeZone,
  }).format(toDate(value))
}

/** Date and time, adding the year only when it is not this year: for history lists. */
export function formatStamp(value: Value, now: Date = new Date()) {
  if (value === null || value === undefined || value === '') return '—'
  const p = parts(value)
  if (p.year === parts(now).year) return formatDateTime(value)
  return `${formatDate(value)}, ${p.hour}:${p.minute}`
}

export function formatTime(value: Value) {
  if (value === null || value === undefined || value === '') return '—'
  if (typeof value === 'string' && /^\d{2}:\d{2}/.test(value)) return value.slice(0, 5)
  const p = parts(value)
  return `${p.hour}:${p.minute}`
}

export function formatWeekday(value: string | Date, style: 'short' | 'long' = 'long') {
  if (isSq()) {
    const p = parts(value)
    return style === 'long' ? SQ.weekdays[p.weekday]! : SQ.weekdaysShort[p.weekday]!
  }
  return new Intl.DateTimeFormat(tag(), { weekday: style }).format(toDate(value))
}

export function formatMonth(value: string | Date) {
  if (isSq()) {
    const p = parts(value)
    return `${SQ.months[p.month - 1]} ${p.year}`
  }
  return new Intl.DateTimeFormat(tag(), { month: 'long', year: 'numeric' }).format(toDate(value))
}

/** "Sep", "sht": for chart axes. */
export function formatMonthShort(value: string | Date) {
  if (isSq()) return SQ.monthsShort[parts(value).month - 1]!
  return new Intl.DateTimeFormat(tag(), { month: 'short' }).format(toDate(value))
}

type Unit = keyof typeof SQ.units

function relativeSq(amount: number, unit: Unit) {
  if (unit === 'second') return 'tani'
  if (unit === 'day' && Math.abs(amount) <= 1)
    return amount === 0 ? 'sot' : amount < 0 ? 'dje' : 'nesër'
  const n = Math.abs(amount)
  const [one, many] = SQ.units[unit]
  return `${amount < 0 ? 'para' : 'pas'} ${n} ${n === 1 ? one : many}`
}

/** "3 days ago", "in 2 weeks" / "para 3 ditësh", "pas 2 javësh" */
export function formatRelative(value: Value, now: Date = new Date()) {
  if (value === null || value === undefined || value === '') return '—'
  const d = toDate(value)
  const diffSeconds = Math.round((d.getTime() - now.getTime()) / 1000)
  const abs = Math.abs(diffSeconds)
  let amount: number
  let unit: Unit
  if (abs < 60) [amount, unit] = [diffSeconds, 'second']
  else if (abs < 3600) [amount, unit] = [Math.round(diffSeconds / 60), 'minute']
  else if (abs < 86400) [amount, unit] = [Math.round(diffSeconds / 3600), 'hour']
  else if (abs < 86400 * 7) [amount, unit] = [Math.round(diffSeconds / 86400), 'day']
  else if (abs < 86400 * 45) [amount, unit] = [Math.round(diffSeconds / (86400 * 7)), 'week']
  else if (abs < 86400 * 365) [amount, unit] = [Math.round(diffSeconds / (86400 * 30)), 'month']
  else [amount, unit] = [Math.round(diffSeconds / (86400 * 365)), 'year']
  if (isSq()) return relativeSq(amount, unit)
  return new Intl.RelativeTimeFormat(tag(), { numeric: 'auto' }).format(amount, unit)
}

/** Albanian digits: groups of three with a no-break space, decimal comma ("1 485,50"). */
function numberSq(n: number, fractionDigits: number) {
  const fixed = Math.abs(n).toFixed(fractionDigits)
  const [whole, fraction] = fixed.split('.')
  const grouped = (whole ?? '0').replace(/\B(?=(\d{3})+(?!\d))/g, NBSP)
  return (n < 0 ? '-' : '') + grouped + (fraction ? ',' + fraction : '')
}

/** Money is always carried as integer cents. */
export function formatMoney(
  cents: number | null | undefined,
  options: { currency?: string; decimals?: boolean } = {},
) {
  if (cents === null || cents === undefined) return '—'
  const decimals = options.decimals ?? cents % 100 !== 0
  const code = options.currency ?? currency
  if (isSq()) {
    const symbol = code === 'EUR' ? '€' : code === 'ALL' ? 'Lekë' : code
    return `${numberSq(cents / 100, decimals ? 2 : 0)}${NBSP}${symbol}`
  }
  return new Intl.NumberFormat(tag(), {
    style: 'currency',
    currency: code,
    minimumFractionDigits: decimals ? 2 : 0,
    maximumFractionDigits: decimals ? 2 : 0,
  }).format(cents / 100)
}

export function formatNumber(value: number | null | undefined, maximumFractionDigits = 1) {
  if (value === null || value === undefined || Number.isNaN(value)) return '—'
  if (isSq()) {
    const rounded = Number(value.toFixed(maximumFractionDigits))
    const digits = Number.isInteger(rounded)
      ? 0
      : Math.min(maximumFractionDigits, String(rounded).split('.')[1]?.length ?? 0)
    return numberSq(rounded, digits)
  }
  return new Intl.NumberFormat(tag(), { maximumFractionDigits }).format(value)
}

export function formatPercent(value: number | null | undefined, maximumFractionDigits = 0) {
  if (value === null || value === undefined || Number.isNaN(value)) return '—'
  if (isSq()) return `${formatNumber(value * 100, maximumFractionDigits)}%`
  return new Intl.NumberFormat(tag(), { style: 'percent', maximumFractionDigits }).format(value)
}

/** "355691234567" -> "+355 69 123 4567" (Albanian mobile grouping, generic otherwise) */
export function formatPhone(value: string | null | undefined) {
  if (!value) return '—'
  const digits = value.replace(/\D/g, '')
  if (digits.startsWith('355') && digits.length === 12) {
    return `+355 ${digits.slice(3, 5)} ${digits.slice(5, 8)} ${digits.slice(8)}`
  }
  if (digits.startsWith('383') && digits.length === 11) {
    return `+383 ${digits.slice(3, 5)} ${digits.slice(5, 8)} ${digits.slice(8)}`
  }
  return '+' + digits.replace(/(\d{3})(?=\d)/g, '$1 ').trim()
}

/** Parse "12,50" or "12.50" typed by a person into cents. */
export function parseMoney(input: string | number | null | undefined): number | null {
  if (input === null || input === undefined || input === '') return null
  if (typeof input === 'number') return Math.round(input * 100)
  const normalized = input.replace(/\s/g, '').replace(',', '.')
  const n = Number(normalized)
  return Number.isFinite(n) ? Math.round(n * 100) : null
}

export function centsToInput(cents: number | null | undefined): string {
  if (cents === null || cents === undefined) return ''
  return (cents / 100).toFixed(2).replace(/\.00$/, '')
}

export function todayIso(): string {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

export function initials(name: string | null | undefined) {
  if (!name) return '?'
  const parts = name.trim().split(/\s+/)
  return (
    (parts[0]?.[0] ?? '') + (parts.length > 1 ? (parts[parts.length - 1]?.[0] ?? '') : '')
  ).toUpperCase()
}
