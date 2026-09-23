import type { Component } from 'vue'
import {
  PhAddressBook,
  PhBarcode,
  PhCalendarCheck,
  PhChatCircleDots,
  PhGearSix,
  PhPhoneCall,
  PhPlus,
  PhSunHorizon,
} from '@phosphor-icons/vue'

export type NavItem = {
  /** route name */
  name: string
  /** i18n key for the label */
  label: string
  icon: Component
  /** shown in the phone's bottom bar (three, around the call to action); others go under "More" */
  primary?: boolean
  /** the one call to action: copper button on desktop, raised centre button on phones */
  cta?: boolean
  /** short label for the phone's bottom bar */
  shortLabel?: string
  /** on desktop, listed in the account menu instead of the band */
  accountMenu?: boolean
  roles?: string[]
}

export const NAV: NavItem[] = [
  { name: 'home', label: 'nav.home', icon: PhSunHorizon, primary: true },
  { name: 'units', label: 'nav.units', icon: PhBarcode, primary: true },
  { name: 'install', label: 'nav.install', shortLabel: 'nav.installShort', icon: PhPlus, cta: true },
  { name: 'due', label: 'nav.due', icon: PhCalendarCheck, primary: true },
  { name: 'bookings', label: 'nav.bookings', icon: PhPhoneCall },
  { name: 'customers', label: 'nav.customers', icon: PhAddressBook },
  { name: 'messages', label: 'nav.messages', icon: PhChatCircleDots, accountMenu: true },
  { name: 'settings', label: 'nav.settings', icon: PhGearSix, accountMenu: true },
]
