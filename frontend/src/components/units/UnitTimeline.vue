<script setup lang="ts">
import { ref, type Component } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  PhBellRinging,
  PhBellSlash,
  PhCalendarPlus,
  PhChatCircleText,
  PhPaperPlaneTilt,
  PhPlugs,
  PhSealCheck,
  PhWrench,
} from '@phosphor-icons/vue'
import UiBadge from '@/components/ui/UiBadge.vue'
import { formatDate, formatDateTime, formatMoney, formatStamp } from '@/lib/format'
import type { TimelineEntry } from '@/lib/types'

defineProps<{ entries: TimelineEntry[] }>()

const { t } = useI18n()
const opened = ref<Set<string>>(new Set())

type Look = {
  icon: Component
  tone: 'primary' | 'accent' | 'success' | 'warning' | 'danger' | 'neutral'
}

function look(e: TimelineEntry): Look {
  switch (e.kind) {
    case 'INSTALLED':
      return { icon: PhSealCheck, tone: 'primary' }
    case 'VISIT':
      return { icon: PhWrench, tone: 'success' }
    case 'REMINDER':
      if (e.outcome === 'NO_CONSENT') return { icon: PhBellSlash, tone: 'warning' }
      if (e.outcome === 'FAILED') return { icon: PhBellSlash, tone: 'danger' }
      return { icon: PhBellRinging, tone: 'accent' }
    case 'BOOKING':
      return { icon: PhCalendarPlus, tone: 'accent' }
    case 'REPLY':
      return { icon: PhChatCircleText, tone: 'neutral' }
    case 'REMOVED':
      return { icon: PhPlugs, tone: 'neutral' }
    default:
      return { icon: PhPaperPlaneTilt, tone: 'neutral' }
  }
}

function title(e: TimelineEntry): string {
  switch (e.kind) {
    case 'INSTALLED':
      return e.by ? t('timeline.INSTALLED_BY', { name: e.by }) : t('timeline.INSTALLED')
    case 'VISIT': {
      const kind = t(`visitKinds.${e.visitKind}`)
      return e.by ? t('timeline.VISIT_BY', { kind, name: e.by }) : kind
    }
    case 'REMINDER':
      if (e.outcome === 'NO_CONSENT') return t('timeline.REMINDER_NO_CONSENT')
      if (e.outcome === 'FAILED') return t('timeline.REMINDER_FAILED')
      if (e.outcome === 'BOOKED') return t('timeline.REMINDER_BOOKED')
      if (e.outcome === 'SERVICED') return t('timeline.REMINDER_SERVICED')
      return t('timeline.REMINDER')
    case 'BOOKING':
      return e.source === 'WHATSAPP' ? t('timeline.BOOKING_WHATSAPP') : t('timeline.BOOKING_CARD')
    case 'MESSAGE':
      return e.templateKey ? t(`timeline.templates.${e.templateKey}`) : t('nav.messages')
    default:
      return t(`timeline.${e.kind}`)
  }
}

function when(e: TimelineEntry): string {
  return e.at && e.kind !== 'VISIT' && e.kind !== 'INSTALLED'
    ? formatStamp(e.at)
    : formatDate(e.date)
}

function key(e: TimelineEntry, i: number) {
  return `${e.kind}-${e.refId ?? i}`
}

function toggle(k: string) {
  const next = new Set(opened.value)
  if (next.has(k)) next.delete(k)
  else next.add(k)
  opened.value = next
}
</script>

<template>
  <ol class="tl">
    <li
      v-for="(e, i) in entries"
      :key="key(e, i)"
      class="tl__item"
      :class="`tl__item--${look(e).tone}`"
    >
      <span class="tl__dot" aria-hidden="true"
        ><component :is="look(e).icon" :size="16" weight="bold"
      /></span>
      <div class="tl__body">
        <div class="tl__head">
          <p class="tl__title">{{ title(e) }}</p>
          <time class="tl__when num">{{ when(e) }}</time>
        </div>

        <p v-if="e.kind === 'REMINDER'" class="tl__detail">
          {{ $t('timeline.cycle', { date: formatDate(e.date) }) }}
        </p>

        <div v-if="e.kind === 'VISIT' && (e.parts || e.priceCents)" class="tl__detail">
          <span v-if="e.parts">{{ e.parts }}</span>
          <span v-if="e.priceCents" class="tl__price num">{{ formatMoney(e.priceCents) }}</span>
        </div>
        <p v-if="e.kind === 'VISIT' && e.notes" class="tl__detail">{{ e.notes }}</p>

        <div v-if="e.kind === 'BOOKING'" class="tl__detail tl__row">
          <UiBadge
            size="sm"
            :tone="
              e.bookingStatus === 'DONE'
                ? 'success'
                : e.bookingStatus === 'DECLINED'
                  ? 'neutral'
                  : 'accent'
            "
          >
            {{ $t(`timeline.bookingStatus.${e.bookingStatus}`) }}
          </UiBadge>
          <span v-if="e.scheduledAt">{{
            $t('unit.scheduledBooking', { when: formatDateTime(e.scheduledAt) })
          }}</span>
          <span v-else-if="e.preferredDate">
            {{
              $t('timeline.prefers', {
                when:
                  formatDate(e.preferredDate, 'short') +
                  (e.preferredPeriod && e.preferredPeriod !== 'ANY'
                    ? ', ' + $t(`periods.${e.preferredPeriod}`).toLowerCase()
                    : ''),
              })
            }}
          </span>
        </div>
        <p v-if="e.kind === 'BOOKING' && e.notes" class="tl__quote">“{{ e.notes }}”</p>

        <p v-if="e.kind === 'REPLY'" class="tl__quote">“{{ e.body }}”</p>

        <template v-if="(e.kind === 'MESSAGE' || e.kind === 'REMINDER') && e.body">
          <button
            type="button"
            class="tl__toggle"
            :aria-expanded="opened.has(key(e, i))"
            @click="toggle(key(e, i))"
          >
            {{ opened.has(key(e, i)) ? $t('timeline.hideMessage') : $t('timeline.showMessage') }}
            <span v-if="e.messageStatus" class="tl__status"
              >· {{ $t(`messages.status.${e.messageStatus}`) }}</span
            >
          </button>
          <p v-if="opened.has(key(e, i))" class="tl__message">{{ e.body }}</p>
        </template>
      </div>
    </li>
  </ol>
</template>

<style scoped>
.tl {
  position: relative;
  display: flex;
  flex-direction: column;
  margin: 0;
  padding: 0;
  list-style: none;
}
.tl__item {
  --tone: var(--gray-500);
  --tone-soft: var(--surface-sunken);
  position: relative;
  display: flex;
  gap: 14px;
  padding-bottom: 18px;
}
.tl__item:not(:last-child)::before {
  content: '';
  position: absolute;
  left: 15px;
  top: 34px;
  bottom: 2px;
  width: 2px;
  border-radius: 2px;
  background: var(--border);
}
.tl__item--primary {
  --tone: var(--primary);
  --tone-soft: var(--primary-soft);
}
.tl__item--accent {
  --tone: var(--accent-600);
  --tone-soft: var(--accent-soft);
}
.tl__item--success {
  --tone: var(--success);
  --tone-soft: var(--success-soft);
}
.tl__item--warning {
  --tone: var(--warning);
  --tone-soft: var(--warning-soft);
}
.tl__item--danger {
  --tone: var(--danger);
  --tone-soft: var(--danger-soft);
}
.tl__dot {
  display: grid;
  place-items: center;
  flex: none;
  width: 32px;
  height: 32px;
  border-radius: 10px;
  color: var(--tone);
  background: var(--tone-soft);
  border: 1px solid color-mix(in srgb, var(--tone) 22%, transparent);
  box-shadow: var(--highlight);
}
.tl__body {
  flex: 1;
  min-width: 0;
  padding-top: 5px;
}
.tl__head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: space-between;
  gap: 2px 12px;
}
.tl__title {
  font-weight: 650;
  font-size: var(--text-sm);
}
.tl__when {
  font-size: var(--text-xs);
  color: var(--text-subtle);
  white-space: nowrap;
}
.tl__detail {
  margin-top: 4px;
  font-size: var(--text-sm);
  color: var(--text-muted);
  display: flex;
  flex-wrap: wrap;
  gap: 4px 12px;
}
.tl__row {
  align-items: center;
}
.tl__price {
  font-weight: 650;
  color: var(--text);
}
.tl__quote {
  margin-top: 6px;
  padding: 6px 10px;
  border-left: 3px solid var(--border-strong);
  color: var(--text-muted);
  font-size: var(--text-sm);
  font-style: italic;
}
.tl__toggle {
  margin-top: 4px;
  padding: 2px 0;
  border: 0;
  background: none;
  color: var(--primary);
  font-size: var(--text-xs);
  font-weight: 650;
}
.tl__toggle:hover {
  text-decoration: underline;
}
.tl__status {
  color: var(--text-subtle);
  font-weight: 500;
}
.tl__message {
  margin-top: 6px;
  padding: 10px 12px;
  border-radius: 4px 12px 12px 12px;
  background: var(--success-soft);
  border: 1px solid color-mix(in srgb, var(--success) 16%, transparent);
  font-size: var(--text-sm);
  white-space: pre-line;
  overflow-wrap: anywhere;
}
</style>
