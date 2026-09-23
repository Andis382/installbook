<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { RouterLink } from 'vue-router'
import {
  PhCalendarCheck,
  PhCheckCircle,
  PhPhone,
  PhQuotes,
  PhWhatsappLogo,
  PhX,
} from '@phosphor-icons/vue'
import UiBadge from '@/components/ui/UiBadge.vue'
import UiButton from '@/components/ui/UiButton.vue'
import UiIconButton from '@/components/ui/UiIconButton.vue'
import TypeIcon from '@/components/units/TypeIcon.vue'
import { formatDate, formatDateTime, formatPhone, formatRelative } from '@/lib/format'
import { telLink, waLink } from '@/lib/whatsapp'
import { town, unitName } from '@/lib/units'
import type { Booking } from '@/lib/types'

const props = withDefaults(defineProps<{ booking: Booking; compact?: boolean }>(), {
  compact: false,
})
defineEmits<{
  schedule: [booking: Booking]
  decline: [booking: Booking]
  done: [booking: Booking]
}>()

const { t } = useI18n()

const preference = computed(() => {
  const b = props.booking
  if (!b.preferredDate)
    return b.preferredPeriod && b.preferredPeriod !== 'ANY'
      ? t(`periods.${b.preferredPeriod}`)
      : t('bookings.anyDay')
  const day = formatDate(b.preferredDate, 'short')
  return b.preferredPeriod && b.preferredPeriod !== 'ANY'
    ? `${day}, ${t(`periods.${b.preferredPeriod}`).toLowerCase()}`
    : day
})
</script>

<template>
  <article
    class="booking"
    :class="[`booking--${booking.status.toLowerCase()}`, { 'booking--compact': compact }]"
  >
    <div class="booking__main">
      <TypeIcon :type="booking.unitType" />
      <div class="booking__who">
        <div class="booking__line">
          <RouterLink
            :to="{ name: 'customer', params: { id: booking.customerId } }"
            class="booking__name"
            >{{ booking.customerName }}</RouterLink
          >
          <span class="booking__phone num">{{ formatPhone(booking.customerPhone) }}</span>
        </div>
        <RouterLink :to="{ name: 'unit', params: { id: booking.unitId } }" class="booking__unit">
          {{ unitName(booking) }} · {{ town(booking.address) }}
        </RouterLink>
        <div class="booking__meta">
          <UiBadge size="sm" :tone="booking.source === 'WHATSAPP' ? 'success' : 'primary'">
            {{ $t(`bookings.source.${booking.source}`) }}
          </UiBadge>
          <span v-if="booking.fromReminder" class="xsmall subtle">{{
            $t('bookings.fromReminder')
          }}</span>
          <span class="xsmall subtle">{{
            $t('bookings.asked', { when: formatRelative(booking.createdAt) })
          }}</span>
        </div>
      </div>
      <div class="booking__when">
        <template v-if="booking.status === 'SCHEDULED' && booking.scheduledAt">
          <span class="eyebrow">{{ $t('bookings.tabs.SCHEDULED') }}</span>
          <strong class="booking__time">{{ formatDateTime(booking.scheduledAt) }}</strong>
        </template>
        <template v-else-if="booking.status === 'NEW'">
          <span class="eyebrow">{{ $t('bookings.prefersLabel') }}</span>
          <strong class="booking__time">{{ preference }}</strong>
        </template>
        <template v-else-if="booking.decidedAt">
          <span class="eyebrow">{{ $t(`bookings.tabs.${booking.status}`) }}</span>
          <strong class="booking__time">{{ formatDate(booking.decidedAt) }}</strong>
        </template>
      </div>
    </div>

    <p v-if="booking.note && !compact" class="booking__note">
      <PhQuotes :size="16" weight="fill" aria-hidden="true" />
      <span>{{ booking.note }}</span>
    </p>

    <div v-if="booking.status === 'NEW' || booking.status === 'SCHEDULED'" class="booking__actions">
      <UiButton
        size="sm"
        variant="secondary"
        :icon="PhPhone"
        :href="telLink(booking.customerPhone) ?? undefined"
        >{{ $t('common.call') }}</UiButton
      >
      <UiButton
        size="sm"
        variant="secondary"
        :icon="PhWhatsappLogo"
        :href="waLink(booking.customerPhone) ?? undefined"
        target="_blank"
      >
        {{ $t('common.whatsapp') }}
      </UiButton>
      <span class="booking__spacer" />
      <template v-if="booking.status === 'NEW'">
        <UiIconButton
          :icon="PhX"
          :label="$t('bookings.decline')"
          size="sm"
          @click="$emit('decline', booking)"
        />
        <UiButton size="sm" :icon="PhCalendarCheck" @click="$emit('schedule', booking)">{{
          $t('bookings.schedule')
        }}</UiButton>
      </template>
      <template v-else>
        <UiButton size="sm" variant="ghost" @click="$emit('schedule', booking)">{{
          $t('bookings.reschedule')
        }}</UiButton>
        <UiButton size="sm" :icon="PhCheckCircle" @click="$emit('done', booking)">{{
          $t('bookings.markDone')
        }}</UiButton>
      </template>
    </div>
  </article>
</template>

<style scoped>
.booking {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: var(--surface);
  box-shadow: var(--shadow-xs);
  transition:
    box-shadow var(--duration) var(--ease),
    transform var(--duration) var(--ease);
}
.booking:hover {
  box-shadow: var(--shadow-md);
}
.booking--new {
  border-left: 3px solid var(--accent);
}
.booking--scheduled {
  border-left: 3px solid var(--success);
}
.booking__main {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.booking__who {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.booking__line {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 4px 10px;
}
.booking__name {
  font-weight: 700;
  color: var(--text);
  text-decoration: none;
}
.booking__name:hover {
  color: var(--primary);
  text-decoration: underline;
}
.booking__phone {
  font-size: var(--text-sm);
  color: var(--text-muted);
}
.booking__unit {
  font-size: var(--text-sm);
  color: var(--text-muted);
  text-decoration: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.booking__unit:hover {
  color: var(--primary);
}
.booking__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin-top: 2px;
}
.booking__when {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
  text-align: right;
  flex: none;
}
.booking__time {
  font-family: var(--font-display);
  font-size: var(--text-md);
  font-weight: 650;
  font-variant-numeric: tabular-nums;
}
.booking__note {
  display: flex;
  gap: 8px;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  background: var(--surface-muted);
  color: var(--text-muted);
  font-size: var(--text-sm);
}
.booking__note :deep(svg) {
  flex: none;
  margin-top: 2px;
  color: var(--gray-400);
}
.booking__actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
}
.booking__spacer {
  flex: 1;
}
@media (max-width: 560px) {
  .booking__main {
    flex-wrap: wrap;
  }
  .booking__when {
    align-items: flex-start;
    text-align: left;
    width: 100%;
    padding-left: 52px;
  }
  .booking__spacer {
    display: none;
  }
  /* call, WhatsApp and the small action on one line; the main action full width below */
  .booking__actions {
    display: grid;
    grid-template-columns: 1fr 1fr auto;
  }
  .booking__actions > :last-child {
    grid-column: 1 / -1;
  }
}
</style>
