<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { PhCalendarCheck, PhSealCheck, PhSealWarning, PhWrench } from '@phosphor-icons/vue'
import UiBadge from '@/components/ui/UiBadge.vue'
import { formatDate, formatMonth } from '@/lib/format'
import { LONG_OVERDUE_DAYS, SERVICE_TONE } from '@/lib/units'
import type { ServiceState, UnitStatus } from '@/lib/types'

/** The two facts that matter about a unit at a glance: its warranty and its next service. */
const props = withDefaults(
  defineProps<{
    warrantyActive: boolean
    warrantyUntil: string
    serviceState: ServiceState | null
    daysUntilDue: number
    nextServiceDue: string
    status?: UnitStatus
    removedOn?: string | null
    only?: 'warranty' | 'service' | null
    size?: 'sm' | 'md'
  }>(),
  { status: 'ACTIVE', removedOn: null, only: null, size: 'md' },
)

const { t } = useI18n()

const service = computed(() => {
  if (props.status === 'REMOVED')
    return {
      tone: 'neutral' as const,
      text: t('pills.removed', { date: formatDate(props.removedOn) }),
    }
  if (!props.serviceState) return null
  const days = props.daysUntilDue
  let text: string
  // weeks late is a count of days; months late is a date to read, not a number to alarm with
  if (props.serviceState === 'OVERDUE')
    text =
      -days > LONG_OVERDUE_DAYS
        ? t('pills.overdueSince', { month: formatMonth(props.nextServiceDue) })
        : t('pills.overdue', { n: -days }, -days)
  else if (props.serviceState === 'DUE_SOON')
    text = days === 0 ? t('pills.dueToday') : t('pills.dueIn', { n: days }, days)
  else text = t('pills.nextService', { date: formatDate(props.nextServiceDue) })
  return { tone: SERVICE_TONE[props.serviceState], text }
})
</script>

<template>
  <span class="pills">
    <UiBadge
      v-if="only !== 'service'"
      :tone="warrantyActive ? 'success' : 'neutral'"
      :icon="warrantyActive ? PhSealCheck : PhSealWarning"
      :size="size"
    >
      {{
        warrantyActive
          ? $t('pills.warrantyUntil', { date: formatDate(warrantyUntil) })
          : $t('pills.warrantyEnded', { date: formatDate(warrantyUntil) })
      }}
    </UiBadge>
    <UiBadge
      v-if="service && only !== 'warranty'"
      :tone="service.tone"
      :icon="serviceState === 'OK' ? PhCalendarCheck : PhWrench"
      :size="size"
    >
      {{ service.text }}
    </UiBadge>
  </span>
</template>

<style scoped>
.pills {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 6px;
}
</style>
