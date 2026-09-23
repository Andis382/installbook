/** Shapes of the InstallBook API. Dates are ISO "YYYY-MM-DD", instants ISO timestamps. */

export type UnitType =
  | 'BOILER'
  | 'AIR_CONDITIONER'
  | 'HEAT_PUMP'
  | 'WATER_HEATER'
  | 'SOLAR_INVERTER'
  | 'ALARM_PANEL'
  | 'OTHER'
export type UnitStatus = 'ACTIVE' | 'REMOVED'
export type ServiceState = 'OVERDUE' | 'DUE_SOON' | 'OK'
export type VisitKind = 'ANNUAL_SERVICE' | 'REPAIR' | 'INSPECTION' | 'WARRANTY_CLAIM'
export type BookingStatus = 'NEW' | 'SCHEDULED' | 'DONE' | 'DECLINED'
export type BookingSource = 'CARD' | 'WHATSAPP'
export type Period = 'MORNING' | 'AFTERNOON' | 'ANY'
export type MessageStatus = 'QUEUED' | 'SENT' | 'DELIVERED' | 'READ' | 'FAILED' | 'SIMULATED'

export const UNIT_TYPES: UnitType[] = [
  'BOILER',
  'AIR_CONDITIONER',
  'HEAT_PUMP',
  'WATER_HEATER',
  'SOLAR_INVERTER',
  'ALARM_PANEL',
  'OTHER',
]
export const VISIT_KINDS: VisitKind[] = ['ANNUAL_SERVICE', 'REPAIR', 'INSPECTION', 'WARRANTY_CLAIM']

export type Message = {
  id: number
  recipient: string
  recipientName: string | null
  templateKey: string
  body: string
  link: string | null
  status: MessageStatus
  error: string | null
  createdAt: string
  waMeUrl: string
}

export type CustomerSummary = {
  id: number
  name: string
  phone: string
  locale: 'sq' | 'en'
  whatsappOptIn: boolean
  whatsappOptInAt: string | null
}

export type UnitRow = {
  id: number
  type: UnitType
  brand: string
  model: string | null
  serialNumber: string | null
  status: UnitStatus
  installedOn: string
  warrantyUntil: string
  warrantyActive: boolean
  nextServiceDue: string
  serviceState: ServiceState | null
  daysUntilDue: number
  address: string
  customerId: number
  customerName: string
  customerPhone: string
}

export type Page<T> = { items: T[]; total: number; page: number; size: number }

export type TimelineEntry = {
  kind: 'INSTALLED' | 'VISIT' | 'REMINDER' | 'BOOKING' | 'MESSAGE' | 'REPLY' | 'REMOVED'
  at: string | null
  date: string | null
  by: string | null
  visitKind: VisitKind | null
  priceCents: number | null
  parts: string | null
  notes: string | null
  outcome: 'SENT' | 'NO_CONSENT' | 'FAILED' | 'BOOKED' | 'SERVICED' | null
  templateKey: string | null
  body: string | null
  messageStatus: MessageStatus | null
  source: BookingSource | null
  preferredDate: string | null
  preferredPeriod: Period | null
  bookingStatus: BookingStatus | null
  scheduledAt: string | null
  refId: number | null
}

export type UnitDetail = {
  id: number
  type: UnitType
  brand: string
  model: string | null
  serialNumber: string | null
  status: UnitStatus
  installedOn: string
  warrantyMonths: number
  warrantyUntil: string
  warrantyActive: boolean
  serviceIntervalMonths: number
  lastServiceOn: string | null
  nextServiceDue: string
  serviceState: ServiceState | null
  daysUntilDue: number
  removedOn: string | null
  address: string
  latitude: number | null
  longitude: number | null
  notes: string | null
  platePhotoId: string | null
  platePhotoUrl: string | null
  installedByName: string | null
  createdAt: string
  cardNumber: string
  cardUrl: string
  cardShareUrl: string
  reminderShareUrl: string
  customer: CustomerSummary
  cycleReminder: {
    dueOn: string
    goesOutOn: string
    outcome: TimelineEntry['outcome']
    sentAt: string | null
  }
  openBooking: {
    id: number
    status: BookingStatus
    preferredDate: string | null
    preferredPeriod: Period | null
    scheduledAt: string | null
    createdAt: string
  } | null
  lastCardMessage: Message | null
  otherUnits: UnitRow[]
  timeline: TimelineEntry[]
}

export type InstallResult = { unit: UnitDetail; cardMessage: Message | null }

export type PlateReading = {
  photoId: string
  photoUrl: string
  aiEnabled: boolean
  reading: {
    type: UnitType | null
    brand: string | null
    model: string | null
    serialNumber: string | null
    notes: string | null
  } | null
}

export type Booking = {
  id: number
  status: BookingStatus
  source: BookingSource
  preferredDate: string | null
  preferredPeriod: Period | null
  note: string | null
  createdAt: string
  scheduledAt: string | null
  decidedAt: string | null
  fromReminder: boolean
  visitId: number | null
  unitId: number
  unitType: UnitType
  brand: string
  model: string | null
  serialNumber: string | null
  address: string
  nextServiceDue: string
  customerId: number
  customerName: string
  customerPhone: string
  whatsappOptIn: boolean
}

export type BookingList = { items: Booking[]; counts: Record<BookingStatus, number> }

export type BookingAction = { booking: Booking; message: Message | null; shareUrl: string | null }

export type ReminderState =
  | 'BOOKING_SCHEDULED'
  | 'BOOKING_REQUESTED'
  | 'SENT'
  | 'NO_CONSENT'
  | 'FAILED'
  | 'WAITING'
  | 'PLANNED'

export type DueRow = {
  unit: UnitRow
  reminderState: ReminderState
  reminderSentAt: string | null
  reminderGoesOutOn: string
  bookingId: number | null
  bookingScheduledAt: string | null
  whatsappOptIn: boolean
  shareUrl: string
}

export type DueGroup = {
  group: 'OVERDUE' | 'THIS_MONTH' | 'NEXT_MONTH'
  month: string | null
  rows: DueRow[]
  estimatedCents: number
}

export type DueView = {
  today: string
  month: string
  currentMonth: string
  typicalPriceCents: number
  groups: DueGroup[]
}

export type RunResult = { sent: number; noConsent: number; failed: number; skipped: number }

export type Dashboard = {
  today: string
  newBookings: Booking[]
  dueThisMonth: number
  overdue: number
  typicalPriceCents: number
  revenueThisMonthCents: number
  revenueOverdueCents: number
  conversion: { sent: number; booked: number; rate: number }
  installsThisMonth: number
  installsLastMonth: number
  installsByMonth: { month: string; count: number }[]
  dueNext: UnitRow[]
  remindersThisWeek: number
  activeUnits: number
}

export type CustomerRow = {
  id: number
  name: string
  phone: string
  locale: 'sq' | 'en'
  whatsappOptIn: boolean
  activeUnits: number
  lastInstalledOn: string | null
}

export type CustomerDetail = {
  id: number
  name: string
  phone: string
  locale: 'sq' | 'en'
  whatsappOptIn: boolean
  whatsappOptInAt: string | null
  whatsappOptOutAt: string | null
  notes: string | null
  createdAt: string
  units: UnitRow[]
  conversation: {
    direction: 'IN' | 'OUT'
    at: string
    body: string | null
    status: string
    templateKey: string | null
  }[]
}

export type CustomerLookup = { found: boolean; customer: CustomerSummary | null; units: UnitRow[] }

export type InstallerSettings = {
  defaultWarrantyMonths: number
  defaultServiceIntervalMonths: number
  reminderLeadDays: number
  typicalServicePriceCents: number
  publicPhone: string | null
}

export type PublicCard = {
  cardNumber: string
  locale: 'sq' | 'en'
  customerName: string
  today: string
  business: { name: string; phone: string | null }
  unit: {
    type: UnitType
    brand: string
    model: string | null
    serialNumber: string | null
    address: string
    installedOn: string
    warrantyMonths: number
    warrantyUntil: string
    warrantyActive: boolean
    serviceIntervalMonths: number
    lastServiceOn: string | null
    nextServiceDue: string
    daysUntilDue: number
    active: boolean
    removedOn: string | null
    platePhotoUrl: string | null
  }
  history: { date: string; kind: VisitKind; parts: string | null }[]
  openBooking: {
    status: BookingStatus
    preferredDate: string | null
    preferredPeriod: Period | null
    scheduledAt: string | null
    createdAt: string
  } | null
}
